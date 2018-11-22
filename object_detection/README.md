# Object detection with Tensorflow Lite on Android
This project is a static images demonstration for object detection API on Android devices. The official demonstration of object detection with Tensorflow Lite  is living in the Camera. To keep the performance, it uses native codes to tracking the objects detected in the Camera previews. The compilation required NDK supports. This project is focusing on the detection in a static image. It simplifies the dependencies of native supports.

### Original project
The original project is included in the official Tensorflow repository:
>[Tensorflow Object Detection API](https://github.com/tensorflow/models/tree/master/research/object_detection)

And, how to run the model on Android devices is also described in the following page:
>[Running on mobile with TensorFlow Lite](https://github.com/tensorflow/models/blob/master/research/object_detection/g3doc/running_on_mobile_tensorflowlite.md)
>

### Tensorflow Lite model
To reduce the complexity of compilation, a pre-downloaded model and result labels are already put in the assets folder of this project. It is downloaded from the link below:
>https://storage.googleapis.com/download.tensorflow.org/models/tflite/coco_ssd_mobilenet_v1_1.0_quant_2018_06_29.zip

The download link is extracted from the source code of original project in Tensorflow repository:
- Before **r1.12** (include **r1.12**), the download links are listed in the file:
[tensorflow/contrib/lite/examples/android/app/download-models.gradle](https://github.com/tensorflow/tensorflow/tree/r1.12/tensorflow/contrib/lite/examples/android/app/download-models.gradle)
- After **r1.12**, the file is moved to:
[tensorflow/blob/master/tensorflow/lite/examples/android/app/download-models.gradle](https://github.com/tensorflow/tensorflow/blob/master/tensorflow/lite/examples/android/app/download-models.gradle)

### Slim API Wrapper
Ease of use for development, we also provide a slim wrapper on original model, [ObjectDetectionModel](https://github.com/dailystudio/ml/blob/master/object_detection/app/src/main/java/com/dailystudio/objectdetection/api/ObjectDetectionModel.java).
To use this API, calling **initialize()** before using it to detect images.
```java
ObjectDetectionModel.isInitialized()
```
And calling **detectImage()** to perform analysis on images:
```java
List<Classifier.Recognition> results =
	ObjectDetectionModel.detectImage(bitmap, .2f);
```
The first parameter is a Bitmap object which is decoded from your image file, while the second parameter is a threshold of detection. Only if the confidence value of the detected object is higher than this threshold, it is will be listed in the results.

### Performance
<img src=".github/object_detection_result_sample.jpg" width="498" height="280" alt="Object detection result"/>

The entire detecting process is performed in a detected thread which is implemented through AsyncTask. Check the code in [DetectAsyncTask.java](https://github.com/dailystudio/ml/blob/master/object_detection/app/src/main/java/com/dailystudio/objectdetection/DetectAsyncTask.java) to see the details. There are three separated phase of detection, decoding, detecting and tagging the results. You can find the performance tracking output through the logcat, here is a sample:

```powershell
D/DetectAsyncTask: doInBackground(): detection is accomplished in 2004ms [decode: 89ms, detect: 74ms, tag: 1841ms].
```

The main part of latency comes from results tagging. I guess that is why the original project required native codes to results tracking in live camera preview. The latency of the object detection will be slightly different among 4 core and 8 core CPUs. It is about 50ms to 100ms. The original size of the input image will not affect the latency of object detection, because we will always scale them to 300 x 300 in dimension before we pass it to Tensorflow model. But high resolution of input will dramatically increase the result tagging latency. Currently, in this project, we scale the input image to make sure it has a dimension smaller than 1920 x1920. The tagging time will be around 1.5 seconds to 2 seconds. If we use a regular photo (e.g. with 3000+ pixels both in width and height), tagging time will be increased to about 15 seconds.
