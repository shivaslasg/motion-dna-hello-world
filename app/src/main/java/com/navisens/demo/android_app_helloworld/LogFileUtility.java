package com.navisens.demo.android_app_helloworld;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class LogFileUtility {

    private Context context;
    private File writeToFile;

    public LogFileUtility(Context context) {
        this.context = context;
    }

    public void createFolderFile(String filename){
        try {
            //Create txt file in SD Card
            File sdCard = Environment.getExternalStorageDirectory();
            //String path = context.getFilesDir().getAbsolutePath();
            File dir = new File(sdCard.getAbsolutePath() +File.separator + "Log File");

            if(!dir.exists()) {
                dir.mkdirs();
            }

            writeToFile = new File(dir, "logcat"+filename+".txt");
        } catch (Exception ex) {
            Log.e("TEST", ex.getMessage());
        }
    }

    public void setWriteToFile(String text){
        try {
            //To write logcat in text file
            FileOutputStream fout = new FileOutputStream(writeToFile, true);
            OutputStreamWriter osw = new OutputStreamWriter(fout);

            //Writing the string to file
            osw.write(text);
            //osw.append(text);
            osw.flush();
            osw.close();
        } catch(FileNotFoundException e) {
            Log.e("TEST", "FileNotFoundException: "+e.getMessage());
            //e.printStackTrace();
        }
        catch(IOException e) {
            Log.e("TEST", "IOException: "+e.getMessage());
            //e.printStackTrace();
        }
        /*try {
            FileWriter fw = new FileWriter(writeToFile,true);
            fw.write(text);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }

    public File[] fetchAllFiles() {
        File sdCard = Environment.getExternalStorageDirectory();
        File myDirectory = new File(sdCard.getAbsolutePath() +File.separator + "Log File");
        File[] directories = myDirectory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                //return pathname.isDirectory();
                return pathname.isFile();
            }
        });
        return directories;
    }
}
