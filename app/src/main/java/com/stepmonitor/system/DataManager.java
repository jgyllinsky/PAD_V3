package com.stepmonitor.system;

import android.os.Bundle;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Used to provide general I/O helper functions
 */
public class DataManager {

    public static final String DEFAULT_DIRECTORY = "/pad/user_data";

    /**
     * Role: Convert the given data to a CSV string
     * @param data List of String array, with each array representing a row and each element a item in a column
     * @return the formatted CSV string
     */
    public static String convertToCSV(List<String[]> data) {
        //take String[] list and convert to CSV
        String output = "";
        for (String[] row : data) {
            String rowOut = "";
            for (String column : row) {
                rowOut += column + ",";
            }
            output += rowOut.substring(0,rowOut.length() - 1) + "\n";
        }
        return output;
    }

    /**
     * Role: convert a HashMap into a json type representation
     * @return a json formatted string where each key and value pair matches that of the passed map
     */
    public static String covertToJson(HashMap<String,String> data) {
        JSONObject json = new JSONObject();
        for (HashMap.Entry<String, String> entry : data.entrySet()) {
            try {
                json.put(entry.getKey(),entry.getValue());
            } catch (org.json.JSONException e) {
                e.printStackTrace();
            }
        }
        return json.toString();
    }

    /**
     * Role: Write the given data string to the given file
     * @param dir the directory to write to relative to the DEFAULT_DIRECTORY
     * @param fileName the file name to use
     * @param data the data to put into the file
     * @param append if true then the file will be appended to, else it is overwritten
     */
    public static void writeToSystem(String dir, String fileName, String data, boolean append) {
        //write the result to storage
        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + DEFAULT_DIRECTORY + dir;
        String filePath = baseDir + File.separator + fileName;

        File path = new File(baseDir);
        FileWriter fw;
        BufferedWriter bw;
        try {
            if (!path.exists()) {
                path.mkdirs();
            }
            File f = new File(filePath);
            if (f.exists() && !f.isDirectory()) {
                fw = new FileWriter(filePath,append);
            } else {
                fw = new FileWriter(filePath);
            }
            bw = new BufferedWriter(fw);
            bw.write(data);
            bw.close();

        } catch (Exception e) {
            Log.e("File Write Error",e.getMessage());
        }
    }

}
