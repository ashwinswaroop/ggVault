package com.ashwin.vault;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.kms.AWSKMSClient;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.kms.model.EncryptRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static android.R.attr.key;
import static android.R.attr.path;
import static android.R.id.input;
import static android.provider.Telephony.Mms.Part.FILENAME;
import static junit.framework.Assert.assertEquals;
import static org.apache.commons.io.FileUtils.readFileToByteArray;

public class FileChooser extends AppCompatActivity {

    private static final int OPEN_DOCUMENT_REQUEST = 1;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 2;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE2 = 5;
    private static final int OPEN_PDF_REQUEST = 3;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 4;
    public static HashMap<String,SecretKeySpec> key = new HashMap<String,SecretKeySpec>();
    public static HashMap<String,IvParameterSpec> ivMap = new HashMap<String,IvParameterSpec>();
    public static ByteBuffer ciphertext;
    public static ByteBuffer plaintext;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_chooser);

        Button button = (Button) findViewById(R.id.open_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDocument();
            }
        });
        ArrayList<String> files = getFileList();
        for(String file : files){
            if(!file.endsWith(".gg")){
                File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)+"/"+file);
                f.delete();
            }
        }
        files = getFileList();
        ArrayAdapter<String> itemsAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, files);
        final ListView listView = (ListView) findViewById(R.id.file_list);
        listView.setAdapter(itemsAdapter);
        listView.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String item = (String) listView.getItemAtPosition(position);
                Log.d("item",item);

                decryptFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/" + item,Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/key.gg");
                String baseFileName = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/" + item;
                File decryptedPdfFile = new File(baseFileName.substring(0, baseFileName.length() - 3));
                if(decryptedPdfFile.exists()) {
                    Uri path = Uri.fromFile(decryptedPdfFile);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    Log.d("Location", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/" + item);
                    Log.d("URI Data", Uri.fromFile(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/" + item)).toString());
                    intent.setDataAndType(path,"application/pdf");
                    intent.setFlags(Intent. FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(Intent.createChooser(intent, "dialogTitle"));
                } else {

                    Toast.makeText(getApplicationContext(), "The file not exists! ", Toast.LENGTH_SHORT).show();

                }
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onResume() {
        super.onResume();
        //File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)+ "/temp.pdf");
        //f.delete();
        ArrayList<String> files = getFileList();
        for(String file : files){
            if(!file.endsWith(".gg")){
                File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)+"/"+file);
                f.delete();
            }
        }
        files = getFileList();
        ArrayAdapter<String> itemsAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, files);
        final ListView listView = (ListView) findViewById(R.id.file_list);
        listView.setAdapter(itemsAdapter);
        listView.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String item = (String) listView.getItemAtPosition(position);
                Log.d("item",item);

                decryptFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/" + item,Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/key.gg");
                String baseFileName = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/" + item;
                File decryptedPdfFile = new File(baseFileName.substring(0, baseFileName.length() - 3));
                if(decryptedPdfFile.exists()) {
                    Uri path = Uri.fromFile(decryptedPdfFile);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    Log.d("Location", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/" + item);
                    Log.d("URI Data", Uri.fromFile(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/" + item)).toString());
                    intent.setDataAndType(path,"application/pdf");
                    intent.setFlags(Intent. FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(Intent.createChooser(intent, "dialogTitle"));
                } else {

                    Toast.makeText(getApplicationContext(), "The file not exists! ", Toast.LENGTH_SHORT).show();

                }
            }
        });
    }

    private void openDocument() {

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), OPEN_DOCUMENT_REQUEST);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == OPEN_DOCUMENT_REQUEST) {
            if (resultCode != RESULT_OK)
                return;
            Cipher desCipher = null;
            Uri uri = data.getData();
            InputStream inputStream;

            try {

                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {

                    // Should we show an explanation?
                    if (shouldShowRequestPermissionRationale(
                            Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        // Explain to the user why we need to read the contacts
                    }

                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE2);

                    // MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE is an
                    // app-defined int constant

                }
                inputStream = getContentResolver().openInputStream(uri);
                File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/" + getFileName(uri));
                FileOutputStream fos = new FileOutputStream(f);
                byte[] buffer = new byte[1024];

                int length;
                while ((length = inputStream.read(buffer)) > 0){
                    fos.write(buffer, 0, length);
                }
                fos.close();
                inputStream.close();
                Log.d("gg", "INSIDE");
                writeKey(56,Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/key.gg","DES");
                encryptFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/" + getFileName(uri),Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/key.gg");
                f.delete();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        /*
        else if(requestCode==OPEN_PDF_REQUEST){
            Uri uri = data.getData();
            StringBuilder text = new StringBuilder();
            InputStream inputStream = null;
            try {
                inputStream = getContentResolver().openInputStream(uri);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    text.append(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        */
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public ArrayList<String> getFileList(){
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (shouldShowRequestPermissionRationale(
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // Explain to the user why we need to read the contacts
            }

            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE2);

            // MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE is an
            // app-defined int constant

        }
        ArrayList<String> fileList = new ArrayList<>();
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getPath();
        Log.d("Files", "Path: " + path);
        File directory = new File(path);
        File[] files = directory.listFiles();
        Log.d("Files", "Size: "+ files.length);
        for (int i = 0; i < files.length; i++)
        {
            fileList.add(files[i].getName());
        }
        return fileList;
    }

    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private String fileExt(String url) {
        if (url.indexOf("?") > -1) {
            url = url.substring(0, url.indexOf("?"));
        }
        if (url.lastIndexOf(".") == -1) {
            return null;
        } else {
            String ext = url.substring(url.lastIndexOf(".") + 1);
            if (ext.indexOf("%") > -1) {
                ext = ext.substring(0, ext.indexOf("%"));
            }
            if (ext.indexOf("/") > -1) {
                ext = ext.substring(0, ext.indexOf("/"));
            }
            return ext.toLowerCase();

        }
    }

    public byte[] file_to_byte_array_java (File f) throws IOException {

        //File file = new File(fileLocation);

        byte[] fileInBytes = new byte[(int) f.length()];

        InputStream inputStream = null;
        try {

            inputStream = new FileInputStream(f);

            inputStream.read(fileInBytes);

        } finally {
            inputStream.close();
        }

        //assertEquals(18, fileInBytes.length);
        return fileInBytes;
    }



    private static void encryptFile(String fileName, String keyName)
    {
        String original = fileName ;
        String encrypted = fileName+".gg";
        Cipher encrypt;
        byte[] initialization_vector = { 22, 33, 11, 44, 55, 99, 66, 77 };
        try
        {
            SecretKey secret_key = readKey(keyName, "DES");
            AlgorithmParameterSpec alogrithm_specs = new IvParameterSpec(initialization_vector);
            encrypt = Cipher.getInstance("DES/CBC/PKCS5Padding");
            encrypt.init(Cipher.ENCRYPT_MODE, secret_key, alogrithm_specs);
            encrypt(new FileInputStream(original), new FileOutputStream(encrypted),encrypt);
            System.out.println("End of Encryption procedure!");
        }
        catch(Exception e)
        {
            System.out.println(e.toString());
        }
    }
    private static void decryptFile(String fileName, String keyName)
    {
        String original = fileName.substring(0, fileName.length() - 3);
        String encrypted = fileName ;
        Cipher decrypt;
        byte[] initialization_vector = { 22, 33, 11, 44, 55, 99, 66, 77 };
        try
        {
            SecretKey secret_key = readKey(keyName, "DES");
            AlgorithmParameterSpec alogrithm_specs = new IvParameterSpec(initialization_vector);

            decrypt = Cipher.getInstance("DES/CBC/PKCS5Padding");
            decrypt.init(Cipher.DECRYPT_MODE, secret_key, alogrithm_specs);
            decrypt(new FileInputStream(encrypted), new FileOutputStream(original),decrypt);
            System.out.println("End of Decryption procedure!");
        }
        catch(Exception e)
        {
            System.out.println(e.toString());
        }
    }

    private static void encrypt(InputStream input, OutputStream output, Cipher encrypt) throws IOException {
        output = new CipherOutputStream(output, encrypt);
        writeBytes(input, output);
    }

    private static void decrypt(InputStream input, OutputStream output,Cipher decrypt) throws IOException {

        input = new CipherInputStream(input, decrypt);
        writeBytes(input, output);
    }

    private static void writeBytes(InputStream input, OutputStream output) throws IOException {
        byte[] writeBuffer = new byte[1024];
        int readBytes = 0;
        while ((readBytes = input.read(writeBuffer)) >= 0) {
            output.write(writeBuffer, 0, readBytes);
        }
        output.close();
        input.close();
    }

    private static void writeKey(int keySize, String output,String algorithm) throws Exception {
        KeyGenerator kg = KeyGenerator.getInstance(algorithm);
        kg.init(keySize);
        System.out.println();
        System.out.println("KeyGenerator Object Info: ");
        System.out.println("Algorithm = " + kg.getAlgorithm());
        System.out.println("Provider = " + kg.getProvider());
        System.out.println("Key Size = " + keySize);
        System.out.println("toString = " + kg.toString());

        SecretKey ky = kg.generateKey();
        byte[] kb;
        FileOutputStream fos = new FileOutputStream(output);
        kb = ky.getEncoded();
        fos.write(kb);

        System.out.println();
        System.out.println("SecretKey Object Info: ");
        System.out.println("Algorithm = " + ky.getAlgorithm());
        System.out.println("Saved File = " + output);
        System.out.println("Size = " + kb.length);
        System.out.println("Format = " + ky.getFormat());
        System.out.println("toString = " + ky.toString());
    }
    private static SecretKey readKey(String input, String algorithm)throws Exception {
        FileInputStream fis = new FileInputStream(input);
        int kl = fis.available();
        byte[] kb = new byte[kl];
        fis.read(kb);
        fis.close();
        KeySpec ks = null;
        SecretKey ky = null;
        SecretKeyFactory kf = null;
        if (algorithm.equalsIgnoreCase("DES")) {
            ks = new DESKeySpec(kb);
            kf = SecretKeyFactory.getInstance("DES");
            ky = kf.generateSecret(ks);
        } else if (algorithm.equalsIgnoreCase("DESede")) {
            ks = new DESedeKeySpec(kb);
            kf = SecretKeyFactory.getInstance("DESede");
            ky = kf.generateSecret(ks);
        } else {
            ks = new SecretKeySpec(kb, algorithm);
            ky = new SecretKeySpec(kb, algorithm);
        }
        /*
        System.out.println();
        System.out.println("KeySpec Object Info: ");
        System.out.println("Saved File = " + fl);
        System.out.println("Length = " + kb.length);
        System.out.println("toString = " + ks.toString());
        System.out.println();
        System.out.println("SecretKey Object Info: ");
        System.out.println("Algorithm = " + ky.getAlgorithm());
        System.out.println("toString = " + ky.toString());
        */
        return ky;
    }
}