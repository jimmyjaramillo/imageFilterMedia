package imageFilterMedia;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.Activity;
import android.database.Cursor;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Log;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.provider.MediaStore;

import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * This class echoes a string called from JavaScript.
 */
public class imageFilterMedia extends CordovaPlugin {
  private JSONObject params;
  private ArrayList<String> listPathFile;
  private Integer resp;
  private Double radEvent;
  private String latitude;
  private String longitude;
  private String latitudeR;
  private String longitudeR;
  private String time;
  private Date dateStart;
  private Date dateFinish;
  private String[] valueLatitude;
  private String[] valueLongitude;
  private String valueLat;
  private String valueLong;
  private String[] valLat;
  private String[] valLong;
  private Double divLat;
  private Double divLong;
  private Double sumLat;
  private Double sumLong;
  private Location locPhoto;
  private Location locEvent;
  private Integer geoPhoto;
  private Integer timePhoto;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
      if (action.equals("getImageByGeoDate")) {
        listPathFile = new ArrayList<String>();
          this.params = args.getJSONObject(0);
          resp = valueFilter(this.params.getString("DataTimeStart"),
                             this.params.getString("DataTimeFinish"),
                             this.params.getString("Latitude"),
                             this.params.getString("Longitude"),
                             this.params.getString("Radius"));

           try {
             dateStart = new Date(this.params.getString("DataTimeStart"));
             dateFinish = new Date(this.params.getString("DataTimeFinish"));
           }catch(Exception e) {
             e.printStackTrace();
           }
           try {
             locEvent = new Location("");
             locEvent.setLatitude(Double.parseDouble(this.params.getString("Latitude")));
             locEvent.setLongitude(Double.parseDouble(this.params.getString("Longitude")));
             radEvent = Double.parseDouble(this.params.getString("Radius"));
           }catch(Exception e) {
             e.printStackTrace();
           }
          InitAsync initAsync = new InitAsync(callbackContext);
          initAsync.execute();
          return true;
      }
        return false;
    }

    public class InitAsync extends AsyncTask<Void, Integer, ArrayList<String>> {

      private CallbackContext callbackContext;
      public InitAsync(CallbackContext callbackContext){
        this.callbackContext = callbackContext;
      }
      protected ArrayList<String> doInBackground(Void... params) {
        try {
          arrayPhotos();
        } catch (IOException e) {
          e.printStackTrace();
        }
        return listPathFile;
      }
      protected  void onPostExecute(ArrayList<String> result) {
        getImageByGeoDate(result, callbackContext);
      }
    }

    public static Integer valueFilter (String dateStart, String dateFinish, String lat, String log, String rad) {
      if (dateStart != "null" && dateFinish != "null" && lat != "null" && log != "null" && rad != "null") {
        return 1;
      }
      if (dateStart != "null" && dateFinish != "null" && lat == "null" && log == "null" && rad == "null") {
        return 2;
      }
      if (dateStart == "null" && dateFinish == "null" && lat != "null" && log != "null" && rad != "null") {
        return 3;
      }
      if (dateStart == "null" && dateFinish == "null" && lat == "null" && log == "null" && rad == "null") {
        return 4;
      }
      return 0;
    }

    public Integer calculateTime (File urlFile) {
      Date time = new Date(urlFile.lastModified());
          if (dateStart.before(time) && dateFinish.after(time)) {
            return 1;
          }
          return 0;
    }

    public Integer calculateGeolocation (String urlFile) {
      try {
        ExifInterface exifInterface = new ExifInterface(urlFile);
        latitude = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
        longitude = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
              latitudeR = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
              longitudeR = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);
      } catch (IOException e) {
        e.printStackTrace();
      }
      if (latitude == null) {
        return 0;
      }
      valueLatitude = latitude.split(",");
          valueLongitude = longitude.split(",");
          valueLat = "";
          valueLong = "";
          valLat = new String[0];
          valLong = new String[0];
          Double sumLat = 0.0;
          Double sumLong = 0.0;
          for (int i = 0; i < valueLatitude.length; i++) {
        valueLat = valueLatitude[i];
        valLat = valueLat.split("/");
        valueLong = valueLongitude[i];
        valLong = valueLong.split("/");
        switch (i) {
          case 0:
               divLat = Double.parseDouble(valLat[0])/Double.parseDouble(valLat[1]);
               divLong = Double.parseDouble(valLong[0])/Double.parseDouble(valLong[1]);
               break;
          case 1:
               divLat = (Double.parseDouble(valLat[0])/Double.parseDouble(valLat[1]))/60;
               divLong = (Double.parseDouble(valLong[0])/Double.parseDouble(valLong[1]))/60;
               break;
          case 2:
               divLat = (Double.parseDouble(valLat[0])/Double.parseDouble(valLat[1]))/3600;
               divLong = (Double.parseDouble(valLong[0])/Double.parseDouble(valLong[1]))/3600;
               break;
        };
        sumLat = sumLat + divLat;
        sumLong = sumLong + divLong;
      }
      if (latitudeR.equals("S")) {
        sumLat = -1 * sumLat;
      }
      if (longitudeR.equals("W")) {
        sumLong = -1 * sumLong;
      }
      locPhoto = new Location("");
          locPhoto.setLatitude(sumLat);
          locPhoto.setLongitude(sumLong);
          float distanceInMeters = locPhoto.distanceTo(locEvent);
      if (distanceInMeters <= radEvent) {
        return 1;
      }
      return 0;
    }


  	private static  Integer validaDir (File file) {
      if (file.isDirectory()) {
          String directoryAux[] = file.getName().split("/");
    			String nombre = directoryAux[directoryAux.length - 1];
          if (nombre.equals("100ANDRO")) {
            return 1;
          }
          if (nombre.equals("Camera")){
            return 1;
          }
        }
        return 0;
    }

    private static Integer validateImage (File file) {
      if (file.isFile()) {
        if (file.getPath().contains(".JPG") || file.getPath().contains(".jpg")){
          return 1;
        }
      }
      return 0;
    }

    public ArrayList arrayPhotos () throws IOException {
      Uri u = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
      String[] projection = {MediaStore.Images.ImageColumns.DATA};
      Cursor c = null;
      SortedSet<String> dirList = new TreeSet<String>();
      String[] directories = null;
      if (u != null)
      {
        c = this.cordova.getActivity().getContentResolver().query(u, projection, null, null, null);
      }
      if ((c != null) && (c.moveToFirst()))
      {
        do
        {
          String tempDir = c.getString(0);
          tempDir = tempDir.substring(0, tempDir.lastIndexOf("/"));
          try{
            dirList.add(tempDir);
          }
          catch(Exception e)
          {

          }
        }
        while (c.moveToNext());
        directories = new String[dirList.size()];
        dirList.toArray(directories);
      }
      for(int i = 0; i < dirList.size(); i++) {
        File currentDirectory = new File(directories[i]);
        switch (validaDir(currentDirectory)){
          case 0:
              break;
          case 1:
              File[] listFile = currentDirectory.listFiles();
              for(File file : listFile) {
                  switch (validateImage(file)) {
                      case 0:
                          break;
                      case 1:
                          switch (resp) {
                              case 0:
                                  break;
                              case 1:
                                  timePhoto = calculateTime(file);
                                  if (timePhoto == 1) {
                                    geoPhoto = calculateGeolocation(file.getAbsolutePath());
                                    if (geoPhoto == 1) {
                                      addPhotosArray(file);
                                    }
                                  }
                                  break;
                              case 2:
                                  timePhoto = calculateTime(file);
                                  if (timePhoto == 1) {
                                    addPhotosArray(file);
                                  }
                                  break;
                              case 3:
                                  geoPhoto = calculateGeolocation(file.getAbsolutePath());
                                  if (geoPhoto == 1) {
                                    addPhotosArray(file);
                                  }
                                  break;
                              case 4:
                                  addPhotosArray(file);
                                  break;
                          }
                          break;
                  }
                }
              break;
        }
      }
      return listPathFile;
    }

    private void  addPhotosArray (File file) throws IOException {
      Bitmap bm = ShrinkBitmap(file.getPath(), 800, 800);
      File aux = storeImage(bm, file.getName());
      listPathFile.add(aux.getPath());
    }

    private File storeImage(Bitmap bmp, String fileName) throws IOException {
      int index = fileName.lastIndexOf('.');
      String name = fileName.substring(0, index);
      String ext = fileName.substring(index);
      File file = File.createTempFile("tmp_" + name, ext);
      OutputStream outStream = new FileOutputStream(file);
      if (ext.compareToIgnoreCase(".png") == 0) {
        bmp.compress(Bitmap.CompressFormat.PNG, 80, outStream);
      } else {
        bmp.compress(Bitmap.CompressFormat.JPEG, 80, outStream);
      }
      outStream.flush();
      outStream.close();
      return file;
    }

    Bitmap ShrinkBitmap(String file, int width, int height){
      BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
      bmpFactoryOptions.inJustDecodeBounds = true;
      Bitmap bitmap = BitmapFactory.decodeFile(file, bmpFactoryOptions);
      int heightRatio = (int)Math.ceil(bmpFactoryOptions.outHeight/(float)height);
      int widthRatio = (int)Math.ceil(bmpFactoryOptions.outWidth/(float)width);
      if (heightRatio > 1 || widthRatio > 1){
        if (heightRatio > widthRatio){
          bmpFactoryOptions.inSampleSize = heightRatio;
        } else {
          bmpFactoryOptions.inSampleSize = widthRatio;
        }
      }
      bmpFactoryOptions.inJustDecodeBounds = false;
      bitmap = BitmapFactory.decodeFile(file, bmpFactoryOptions);
      return bitmap;
    }
    private void getImageByGeoDate(ArrayList<String> listImages, CallbackContext callbackContext) {
        if (listImages.size() > 0) {
          JSONArray res = new JSONArray(listImages);
            callbackContext.success(res);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }
}
