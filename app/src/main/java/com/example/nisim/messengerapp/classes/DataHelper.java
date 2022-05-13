package com.example.nisim.messengerapp.classes;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DataHelper {
    /*
    Public Methods
     */
    public static ArrayList<Message> filterMessages(DataSnapshot dataSnapshot) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        ArrayList<Message> messages = new ArrayList<>();

        for (DataSnapshot data : dataSnapshot.child("Messages").getChildren()) {
            Message message = data.getValue(Message.class);

            if (firebaseUser.getUid().equals(message.getFromUserUid()) ||
                    firebaseUser.getUid().equals(message.getToUserUid())) {
                messages.add(message);
            }
        }

        return messages;
    }

    public static String objectToJson(Object object) {
        Gson gson = new Gson();
        String json = gson.toJson(object);
        return json;
    }

    public static Object JsonToObject(String json, String className) {
        if (json != null) {
            Gson gson = new Gson();
            Class tempClass = null;
            try {
                tempClass = Class.forName("com.example.nisim.messengerapp.classes." + className);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            Object classObject = null;
            try {
                classObject = tempClass.newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            Object obj = gson.fromJson(json, classObject.getClass());
            return obj;
        }

        return null;
    }

    public static void saveUsersToInternalStorage(Context context, Map<String, User> users) {
        saveMapToInternalStorage(context, "users", users);
    }

    public static void saveMessagesToInternalStorage(Context context, ArrayList<Message> messages) {
        saveArrayListToInternalStorage(context, "messages", messages);
    }

    public static Map<String, User> loadUsersFromInternalStorage(Context context) {
        String jsonString = loadJsonFromInternalStorage(context, "users");
        return JsonToMapUser(jsonString);
    }

    public static ArrayList<Message> loadMessagesFromInternalStorage(Context context) {
        String jsonString = loadJsonFromInternalStorage(context, "messages");
        return JsonToArrayListMessage(jsonString);
    }

    public static void deleteDataFromInternalStorage(Context context) {
        saveArrayListToInternalStorage(context, "messages", new ArrayList<>());
        saveMapToInternalStorage(context, "users", new HashMap<>());
    }

    public static void saveToCache(Context context, String filename, String str) {
        saveToFile(context, context.getCacheDir() + "/" + filename, str);
    }

    public static void saveProfilePhotoToStorage(Context context, String filename, Uri uri, int sizeDps) {
        //minimize photo to thumbnail before saving it in storage
        Uri fixedUri = PhotoHelper.createThumbnailFromUri(context, uri, PhotoHelper.dpsToPixels(context, sizeDps), PhotoHelper.dpsToPixels(context, sizeDps));
        saveToFile(context, "profilePhotos", filename, fixedUri);
    }

    public static void deleteProfilePhotoFromStorage(Context context, String filename) {
        deleteFile(context, "profilePhotos", filename);
    }

    public static String loadFromCache(Context context, String filename) {
        return loadFromFile(context.getCacheDir() + "/" + filename);
    }

    public static Uri loadProfilePhotoFromStorage(Context context, String filename) {
        return loadFromFile(context, "profilePhotos", filename);
    }

    /*
    Private Methods
     */
    private static void saveArrayListToInternalStorage(Context context, String filename, ArrayList<?> arrayList) {
        saveToFile(context, context.getFilesDir() + "/" + filename, ArrayListToJson(arrayList));
    }

    private static void saveMapToInternalStorage(Context context, String filename, Map<?, ?> map) {
        saveToFile(context, context.getFilesDir() + "/" + filename, MapToJson(map));
    }

    private static String loadJsonFromInternalStorage(Context context, String filename) {
        return loadFromFile(context.getFilesDir() + "/" + filename);
    }

    private static String ArrayListToJson(ArrayList<?> list) {
        Gson gson = new Gson();
        String json = gson.toJson(list);
        return json;
    }

    private static String MapToJson(Map<?, ?> map) {
        Gson gson = new Gson();
        String json = gson.toJson(map);
        return json;
    }

    private static ArrayList<Message> JsonToArrayListMessage(String json) {
        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<Message>>() {
            }.getType();
            try {
                return gson.fromJson(json, type);
            } catch (Exception e) { //corrupted file
                e.printStackTrace();
            }
        }

        return new ArrayList<>();
    }

    private static Map<String, User> JsonToMapUser(String json) {
        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, User>>() {
            }.getType();
            try {
                return gson.fromJson(json, type);
            } catch (Exception e) { //corrupted file
                e.printStackTrace();
            }
        }

        return new HashMap<>();
    }

    private static Uri loadFromFile(Context context, String path, String filename) {
        File filesDirectory = new File(context.getFilesDir(), path);
        File localFile = new File(filesDirectory, filename);

        if (localFile.exists() && localFile.length() > 0) {
            return Uri.fromFile(localFile);
        }

        return null;
    }

    private static String loadFromFile(String path) {
        File file = new File(path); //get chats dir
        if (!file.exists()) //check if file exists (prevent error)
        {
            return null;
        }

        //read from byte file
        int size = (int) file.length();
        byte bytes[] = new byte[size];
        byte tmpBuff[] = new byte[size];
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        int read = 0;
        try {
            read = fis.read(bytes, 0, size);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (read < size) { //prevents loss of data
            int remain = size - read;
            while (remain > 0) {
                try {
                    read = fis.read(tmpBuff, 0, remain);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.arraycopy(tmpBuff, 0, bytes, size - remain, read);
                remain -= read;
            }
        }

        //convert bytes to String
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static void saveToFile(Context context, String path, String str) {
        File file = new File(path);
        FileOutputStream fos = null;
        Writer out = null;
        try {
            fos = new FileOutputStream(file);
            out = new OutputStreamWriter(fos, "UTF-8");
            out.write(str);
            out.flush();
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ignored) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static void saveToFile(Context context, String path, String filename, Uri uri) {
        File filesDirectory = new File(context.getFilesDir(), path);
        File destination = new File(filesDirectory, filename);
        File source = new File(uri.getPath());

        try {
            FileUtils.copyFile(source, destination);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean deleteFile(Context context, String path, String filename) {
        File filesDirectory = new File(context.getFilesDir(), path);
        File file = new File(filesDirectory, filename);
        return file.delete();
    }
}