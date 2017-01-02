package com.ashwin.vault;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import android.support.v7.app.ActionBar.LayoutParams;
import android.support.v7.widget.Toolbar;

import static junit.framework.Assert.assertEquals;

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
    public KeyDBHelper keyDbHelper = new KeyDBHelper(this);
    public PasswordDBHelper passwordDbHelper = new PasswordDBHelper(this);
    ArrayList<String> filesWithPermission = new ArrayList<String>();

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_chooser);
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
        ArrayList<String> files = new ArrayList<String>();
        if(passwordDbHelper.isEmpty()){
            new File("/storage/emulated/0/ggVault").mkdir();
            files = filesWithPermission;
            DialogFragment dF = new CreateVaultFragment();
            dF.setCancelable(false);
            dF.show(getFragmentManager(),"CreateVaultFragment");
        }
        else{
            files = getFileList();
            DialogFragment dF = new EnterVaultFragment();
            dF.setCancelable(false);
            dF.show(getFragmentManager(),"EnterVaultFragment");
        }
        FloatingActionButton button = (FloatingActionButton) findViewById(R.id.fab);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDocument();
            }
        });
        for(String file : files){
            if(!file.endsWith(".gg")){
                File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)+"/"+file);
                f.delete();
            }
        }
        //files = filesWithPermission;
        //files.remove(0);

        //ArrayAdapter<String> itemsAdapter =
                //new ArrayAdapter<>(this, R.layout.list_row, files);
        CustomAdapter adapter = new CustomAdapter(this, files);
        final ListView listView = (ListView) findViewById(R.id.file_list);
        listView.setAdapter(adapter);
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
                    intent.setDataAndType(path,"*/*");
                    intent.setFlags(Intent. FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(Intent.createChooser(intent, "Open with.."));
                } else {

                    Toast.makeText(getApplicationContext(), "The file does not exist! ", Toast.LENGTH_SHORT).show();

                }
            }
        });

        final ArrayList<String> finalFiles = files;
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, final long id) {

                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                // Yes button clicked
                                //Toast.makeText(MainActivity.this, "Yes Clicked",
                                //Toast.LENGTH_LONG).show();
                                String item = (String) listView.getItemAtPosition(position);
                                File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/" + item);
                                f.delete();
                                finalFiles.remove(position);
                                CustomAdapter adapter = new CustomAdapter(FileChooser.this, finalFiles);
                                final ListView listView = (ListView) findViewById(R.id.file_list);
                                listView.setAdapter(adapter);
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                // No button clicked
                                // do nothing
                                //Toast.makeText(MainActivity.this, "No Clicked",
                                //Toast.LENGTH_LONG).show();
                                break;
                        }
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(FileChooser.this);
                builder.setMessage("Remove encrypted file from vault?")
                        .setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();

                return true;
            }
        });

        android.support.v7.app.ActionBar mActionBar = getSupportActionBar();
        mActionBar.setDisplayShowHomeEnabled(false);
        mActionBar.setDisplayShowTitleEnabled(false);
        LayoutInflater mInflater = LayoutInflater.from(this);

        View mCustomView = mInflater.inflate(R.layout.custom_actionbar, null);
        TextView mTitleTextView = (TextView) mCustomView.findViewById(R.id.title_text);
        mTitleTextView.setText("ggVault");

        ImageButton imageButton = (ImageButton) mCustomView
                .findViewById(R.id.imageButton);
        imageButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                System.exit(0);
            }
        });

        mActionBar.setCustomView(mCustomView);
        mActionBar.setDisplayShowCustomEnabled(true);

        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);
        getSupportActionBar().setCustomView(mCustomView, layoutParams);
        Toolbar parent = (Toolbar) mCustomView.getParent();
        parent.setContentInsetsAbsolute(0, 0);

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onResume() {
        super.onResume();
        //File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)+ "/temp.pdf");
        //f.delete();

        ArrayList<String> files = new ArrayList<String>();
        if(passwordDbHelper.isEmpty()){
            files = filesWithPermission;
        }
        else{
            files = getFileList();
        }
        for(String file : files){
            if(!file.endsWith(".gg")){
                File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)+"/"+file);
                f.delete();
            }
        }
        //files.remove(0);
        //ArrayAdapter<String> itemsAdapter =
                //new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, files);
        CustomAdapter adapter = new CustomAdapter(this, files);
        final ListView listView = (ListView) findViewById(R.id.file_list);
        listView.setAdapter(adapter);
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
                    intent.setDataAndType(path,"*/*");
                    intent.setFlags(Intent. FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(Intent.createChooser(intent, "Open with.."));
                } else {

                    Toast.makeText(getApplicationContext(), "The file does not exist! ", Toast.LENGTH_SHORT).show();

                }
            }
        });

        final ArrayList<String> finalFiles = files;
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, final long id) {

                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                // Yes button clicked
                                //Toast.makeText(MainActivity.this, "Yes Clicked",
                                //Toast.LENGTH_LONG).show();
                                String item = (String) listView.getItemAtPosition(position);
                                File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/" + item);
                                f.delete();
                                finalFiles.remove(position);
                                CustomAdapter adapter = new CustomAdapter(FileChooser.this, finalFiles);
                                final ListView listView = (ListView) findViewById(R.id.file_list);
                                listView.setAdapter(adapter);
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                // No button clicked
                                // do nothing
                                //Toast.makeText(MainActivity.this, "No Clicked",
                                //Toast.LENGTH_LONG).show();
                                break;
                        }
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(FileChooser.this);
                builder.setMessage("Remove encrypted file from vault?")
                        .setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();

                return true;
            }
        });

        android.support.v7.app.ActionBar mActionBar = getSupportActionBar();
        mActionBar.setDisplayShowHomeEnabled(false);
        mActionBar.setDisplayShowTitleEnabled(false);
        LayoutInflater mInflater = LayoutInflater.from(this);

        View mCustomView = mInflater.inflate(R.layout.custom_actionbar, null);
        TextView mTitleTextView = (TextView) mCustomView.findViewById(R.id.title_text);
        mTitleTextView.setText("ggVault");

        ImageButton imageButton = (ImageButton) mCustomView
                .findViewById(R.id.imageButton);
        imageButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                System.exit(0);
            }
        });

        mActionBar.setCustomView(mCustomView);
        mActionBar.setDisplayShowCustomEnabled(true);

        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);
        getSupportActionBar().setCustomView(mCustomView, layoutParams);
        Toolbar parent = (Toolbar) mCustomView.getParent();
        parent.setContentInsetsAbsolute(0, 0);
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
                if(keyDbHelper.isEmpty()){
                    writeKey(56,Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/key.gg","DES");
                }
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

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE2: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

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
                    filesWithPermission = fileList;
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
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



    private void encryptFile(String fileName, String keyName)
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
    private void decryptFile(String fileName, String keyName)
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

    private void writeKey(int keySize, String output,String algorithm) throws Exception {
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
        //FileOutputStream fos = new FileOutputStream(output);
        kb = ky.getEncoded();
        keyDbHelper.addKey(kb);
        //fos.write(kb);

        System.out.println();
        System.out.println("SecretKey Object Info: ");
        System.out.println("Algorithm = " + ky.getAlgorithm());
        System.out.println("Saved File = " + output);
        System.out.println("Size = " + kb.length);
        System.out.println("Format = " + ky.getFormat());
        System.out.println("toString = " + ky.toString());
    }
    private SecretKey readKey(String input, String algorithm)throws Exception {
        /*
        FileInputStream fis = new FileInputStream(input);
        int kl = fis.available();
        byte[] kb = new byte[kl];
        fis.read(kb);
        fis.close();
        */
        byte[] kb = keyDbHelper.getKey();
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
    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.help:
                return true;
            case R.id.exit:
                System.exit(0);
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    */


}