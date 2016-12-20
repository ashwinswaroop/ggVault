package com.ashwin.vault;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
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
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
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
    private static final int OPEN_FILE_REQUEST = 3;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 4;
    public static HashMap<String,SecretKeySpec> key = new HashMap<String,SecretKeySpec>();
    public static HashMap<String,IvParameterSpec> ivMap = new HashMap<String,IvParameterSpec>();

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
        ArrayAdapter<String> itemsAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, files);
        final ListView listView = (ListView) findViewById(R.id.file_list);
        listView.setAdapter(itemsAdapter);
        listView.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String item = (String) listView.getItemAtPosition(position);
                Log.d("item",item);
                BasicAWSCredentials creds = new BasicAWSCredentials("AKIAJSHAOQIPWO3JZUXQ", "E5YWncNcYgDBjykpWa9KT9DMmUbNukK6VTXiv2aE");
                AWSKMSClient kms = new AWSKMSClient(creds);
                File pdfFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)+ "/" + item);
                byte[] encryptedBytes = null;
                ByteBuffer encryptedByteBuffer = null;
                try {
                    encryptedBytes = FileUtils.readFileToByteArray(pdfFile);
                    encryptedByteBuffer = ByteBuffer.wrap(encryptedBytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                DecryptRequest req = new DecryptRequest().withCiphertextBlob(encryptedByteBuffer);
                ByteBuffer decryptedByteBuffer = kms.decrypt(req).getPlaintext();
                File decryptedPdfFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)+ "/temp.pdf");
                FileChannel wChannel = null;
                try {
                    wChannel = new FileOutputStream(decryptedPdfFile, false).getChannel();
                    wChannel.write(decryptedByteBuffer);
                    wChannel.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

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
        ArrayAdapter<String> itemsAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, files);
        final ListView listView = (ListView) findViewById(R.id.file_list);
        listView.setAdapter(itemsAdapter);
        listView.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String item = (String) listView.getItemAtPosition(position);
                Log.d("item",item);
                BasicAWSCredentials creds = new BasicAWSCredentials("AKIAJSHAOQIPWO3JZUXQ", "E5YWncNcYgDBjykpWa9KT9DMmUbNukK6VTXiv2aE");
                AWSKMSClient kms = new AWSKMSClient(creds);
                File pdfFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)+ "/" + item);
                byte[] encryptedBytes = null;
                ByteBuffer encryptedByteBuffer = null;
                try {
                    encryptedBytes = FileUtils.readFileToByteArray(pdfFile);
                    encryptedByteBuffer = ByteBuffer.wrap(encryptedBytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                DecryptRequest req = new DecryptRequest().withCiphertextBlob(encryptedByteBuffer);
                ByteBuffer decryptedByteBuffer = kms.decrypt(req).getPlaintext();
                File decryptedPdfFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)+ "/temp.pdf");
                FileChannel wChannel = null;
                try {
                    wChannel = new FileOutputStream(decryptedPdfFile, false).getChannel();
                    wChannel.write(decryptedByteBuffer);
                    wChannel.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

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

                Log.d("gg", "INSIDE");
                inputStream = getContentResolver().openInputStream(uri);
                BasicAWSCredentials creds = new BasicAWSCredentials("AKIAJSHAOQIPWO3JZUXQ", "E5YWncNcYgDBjykpWa9KT9DMmUbNukK6VTXiv2aE");
                AWSKMSClient kms = new AWSKMSClient(creds);
                String keyId = "arn:aws:kms:us-east-1:147457270036:key/52f6f49d-8bfd-4ec5-8c5e-9085c4acc583";
                EncryptRequest req = new EncryptRequest().withKeyId(keyId).withPlaintext(ByteBuffer.wrap(IOUtils.toByteArray(inputStream)));
                ByteBuffer ciphertext = kms.encrypt(req).getCiphertextBlob();
                File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/" + getFileName(uri));
                FileChannel wChannel = new FileOutputStream(f, false).getChannel();
                wChannel.write(ciphertext);
                wChannel.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        /*
        else if(requestCode==OPEN_FILE_REQUEST){
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

}
