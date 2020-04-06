package com.TrakEngineering.FluidSecureHubTest.Camera2SecretPictureTaker;


import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.TreeMap;


@TargetApi(Build.VERSION_CODES.LOLLIPOP) //NOTE: camera 2 api was added in API level 21
public class PictureCapturingServiceImpl extends APictureCapturingService {

    private static final String TAG = PictureCapturingServiceImpl.class.getSimpleName();

    private CameraDevice cameraDevice;
    private ImageReader imageReader;
    /***
     * camera ids queue.
     */
    private Queue<String> cameraIds;

    private String currentCameraId;
    private boolean cameraClosed;
    /**
     * stores a sorted map of (pictureUrlOnDisk, PictureData).
     */
    private TreeMap<String, byte[]> picturesTaken;
    private PictureCapturingListener capturingListener;

    /***
     * private constructor, meant to force the use of {@link #getInstance}  method
     */
    private PictureCapturingServiceImpl(final Activity activity) {
        super(activity);
    }

    /**
     * @param activity the activity used to get the app's context and the display manager
     * @return a new instance
     */
    public static APictureCapturingService getInstance(final Activity activity) {
        return new PictureCapturingServiceImpl(activity);
    }

    /**
     * Starts pictures capturing treatment.
     *
     * @param listener picture capturing listener
     */
    @Override
    public void startCapturing(final PictureCapturingListener listener) {
        this.picturesTaken = new TreeMap<>();
        this.capturingListener = listener;
        this.cameraIds = new LinkedList<>();
        try {
            final String[] cameraIds = manager.getCameraIdList();


            String fCameraId = getFrontFacingCameraId(manager);


            if (cameraIds.length > 0) {

                this.cameraIds.addAll(Collections.singleton(fCameraId));
                this.currentCameraId = this.cameraIds.poll();
                openCamera();

            } else {
                //No camera detected!
                capturingListener.onDoneCapturingAllPhotos(picturesTaken);
            }
        } catch (final CameraAccessException e) {
            Log.e(TAG, "Exception occurred while accessing the list of cameras", e);
        }
    }

    String getFrontFacingCameraId(CameraManager cManager){
        try {
            for(final String cameraId : cManager.getCameraIdList()){
                CameraCharacteristics characteristics = cManager.getCameraCharacteristics(cameraId);
                int cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
                if(cOrientation == CameraCharacteristics.LENS_FACING_FRONT) return cameraId;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.e(TAG, "Exception occurred while getting front camera id", e);
        }
        return null;
    }

    private void openCamera() {
        Log.d(TAG, "opening camera " + currentCameraId);
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                manager.openCamera(currentCameraId, stateCallback, null);
            }
        } catch (final CameraAccessException e) {
            Log.e(TAG, " exception occurred while opening camera " + currentCameraId, e);
        }
    }

    private final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            if (picturesTaken.lastEntry() != null) {
                capturingListener.onCaptureDone(picturesTaken.lastEntry().getKey(), picturesTaken.lastEntry().getValue());
                Log.i(TAG, "done taking picture from camera " + cameraDevice.getId());
            }
            closeCamera();
        }
    };


    private final ImageReader.OnImageAvailableListener onImageAvailableListener = (ImageReader imReader) -> {
        final Image image = imReader.acquireLatestImage();
        final ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        final byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);

        byte[] rotatedImage = rotateImage(bytes);

        //saveImageToDisk(rotatedImage);
        new saveImage().execute(rotatedImage);
        image.close();
    };

    private byte[] rotateImage(byte[] bytes) {
        //To roate an image captured from camera since the image gets auto rotated
        InputStream is = new ByteArrayInputStream(bytes);
        Bitmap bitmap = BitmapFactory.decodeStream(is);

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Matrix mtx = new Matrix();
        mtx.postRotate(270);
        // Rotating Bitmap
        Bitmap rotatedBMP = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        rotatedBMP.compress(Bitmap.CompressFormat.PNG, 100, stream);
        bytes = stream.toByteArray();
        return bytes;
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraClosed = false;
            Log.d(TAG, "camera " + camera.getId() + " opened");
            cameraDevice = camera;
            Log.i(TAG, "Taking picture from camera " + camera.getId());
            //Take the picture after some delay. It may resolve getting a black dark photos.
            new Handler().postDelayed(() -> {
                try {
                    Log.i(TAG, "Taking picture from camera before takePicture() " + camera.getId());
                    takePicture();
                } catch (final CameraAccessException e) {
                    Log.e(TAG, " exception occurred while taking picture from " + currentCameraId, e);
                }
            }, 500);
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.d(TAG, " camera " + camera.getId() + " disconnected");
            if (cameraDevice != null && !cameraClosed) {
                cameraClosed = true;
                cameraDevice.close();
            }
        }

        @Override
        public void onClosed(@NonNull CameraDevice camera) {
            cameraClosed = true;
            Log.d(TAG, "camera " + camera.getId() + " closed");
            //once the current camera has been closed, start taking another picture
            if (!cameraIds.isEmpty()) {
                takeAnotherPicture();
            } else {
                capturingListener.onDoneCapturingAllPhotos(picturesTaken);
            }
        }


        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(TAG, "camera in error, int code " + error);
            if (cameraDevice != null && !cameraClosed) {
                cameraDevice.close();
            }
        }
    };


    private void takePicture() throws CameraAccessException {
        if (null == cameraDevice) {
            Log.e(TAG, "cameraDevice is null");
            return;
        }
        Log.i(TAG, "Inside takePicture " + cameraDevice.getId());
        final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
        Size[] jpegSizes = null;
        StreamConfigurationMap streamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (streamConfigurationMap != null) {
            jpegSizes = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG);
        }
        final boolean jpegSizesNotEmpty = jpegSizes != null && 0 < jpegSizes.length;
        int width = jpegSizesNotEmpty ? jpegSizes[0].getWidth() : 480;
        int height = jpegSizesNotEmpty ? jpegSizes[0].getHeight() : 640;
        final ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
        final List<Surface> outputSurfaces = new ArrayList<>();
        outputSurfaces.add(reader.getSurface());
        final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        captureBuilder.addTarget(reader.getSurface());
        captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation());
        reader.setOnImageAvailableListener(onImageAvailableListener, null);
        cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        try {
                            Log.i(TAG, "Inside takePicture try block " + cameraDevice.getId());
                            session.capture(captureBuilder.build(), captureListener, null);
                        } catch (final CameraAccessException e) {
                            Log.e(TAG, " exception occurred while accessing " + currentCameraId, e);
                        }
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    }
                }
                , null);
    }

    public class saveImage extends AsyncTask<byte[], Void, String>{

        OutputStream imageFileOS;

        @Override
        protected String doInBackground(byte[]... params) {

            byte[] data = params[0];

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = "IMG_" + timeStamp;
            File path = new File(Environment.getExternalStorageDirectory() + "/FSPictureData");

            if (!path.exists()) {
                File wallpaperDirectory = new File("/sdcard/FSPictureData/");
                wallpaperDirectory.mkdirs();
            }

            File file = new File(new File("/sdcard/FSPictureData/"), fileName + ".png");

            //To roate an image captured from camera since the image gets auto rotated
            /*InputStream is = new ByteArrayInputStream(data);
            Bitmap bitmap = BitmapFactory.decodeStream(is);

            int w = bitmap.getWidth();
            int h = bitmap.getHeight();

            Matrix mtx = new Matrix();
            mtx.postRotate(270);
            // Rotating Bitmap
            Bitmap rotatedBMP = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            rotatedBMP.compress(Bitmap.CompressFormat.PNG, 100, stream);
            data = stream.toByteArray();*/
            //To roate ends-------

            try {

                //imageFileOS = getContentResolver().openOutputStream(uriTarget);
                imageFileOS = new FileOutputStream(file);
                imageFileOS.write(data);
                imageFileOS.flush();
                imageFileOS.close();

                // Bitmap bmp = BitmapFactory.decodeByteArray(data,0,data.length);
                // createDirectoryAndSaveFile(uriTarget,bmp,fileName);

                // Toast.makeText(getApplicationContext(), "Image saved: " + file.toString(), Toast.LENGTH_LONG).show();

            } catch (FileNotFoundException e) {
                e.printStackTrace();

            } catch (IOException e) {
                e.printStackTrace();

            }


            /*try (final OutputStream output = new FileOutputStream(file)) {
                output.write(data);
                this.picturesTaken.put(file.getPath(), data);
            } catch (final IOException e) {
                Log.e(TAG, "Exception occurred while saving picture to external storage ", e);
            }*/


            return null;
        }
    }

    private void saveImageToDisk( byte[] data) {


        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = "IMG_" + timeStamp;
        File path = new File(Environment.getExternalStorageDirectory() + "/FSPictureData");

        if (!path.exists()) {
            File wallpaperDirectory = new File("/sdcard/FSPictureData/");
            wallpaperDirectory.mkdirs();
        }

        File file = new File(new File("/sdcard/FSPictureData/"), fileName + ".png");

        //To roate an image captured from camera since the image gets auto rotated
        /*InputStream is = new ByteArrayInputStream(data);
        Bitmap bitmap = BitmapFactory.decodeStream(is);

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Matrix mtx = new Matrix();
        mtx.postRotate(270);
        // Rotating Bitmap
        Bitmap rotatedBMP = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        rotatedBMP.compress(Bitmap.CompressFormat.PNG, 100, stream);
        data = stream.toByteArray();*/
        //To roate ends-------

        try (final OutputStream output = new FileOutputStream(file)) {
            output.write(data);
            this.picturesTaken.put(file.getPath(), data);
        } catch (final IOException e) {
            Log.e(TAG, "Exception occurred while saving picture to external storage ", e);
        }



    }

    private void takeAnotherPicture() {
        this.currentCameraId = this.cameraIds.poll();
        openCamera();
    }

    private void closeCamera() {
        Log.d(TAG, "closing camera " + cameraDevice.getId());
        if (null != cameraDevice && !cameraClosed) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }


}