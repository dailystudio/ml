# Object detection with Tensorflow Lite on Android
This project is a static images demonstration for object detection API on Android devices. The official demonstration of object detection with Tensorflow Lite  is living in the Camera. To keep the performance, it uses native codes to tracking the objects detected in the Camera previews. The compilation required NDK supports. This project is focusing on the detection in a static image. It simplifies the dependencies of native supports.

#### Original project
The original project is included in the official Tensorflow repository:
>[Tensorflow Object Detection API](https://github.com/tensorflow/models/tree/master/research/object_detection)

And, how to run the model on Android devices is also described in the following page:
>[Running on mobile with TensorFlow Lite](https://github.com/tensorflow/models/blob/master/research/object_detection/g3doc/running_on_mobile_tensorflowlite.md)
>

#### Tensorflow Lite model
For convenience, a pre-downloaded model are already put in the assets folder of this project. It is downloaded from the link below:
>https://storage.googleapis.com/download.tensorflow.org/models/tflite/coco_ssd_mobilenet_v1_1.0_quant_2018_06_29.zip

The download link is extracted from the source code of original project in Tensorflow repository:
- Before **r1.12** (include **r1.12**), the download links are listed in the file
[tensorflow/contrib/lite/examples/android/app/download-models.gradle](https://github.com/tensorflow/tensorflow/tree/r1.12/tensorflow/contrib/lite/examples/android/app/download-models.gradle)
- After **r1.12**, the download links are moved to:
[tensorflow/blob/master/tensorflow/lite/examples/android/app/download-models.gradle](https://github.com/tensorflow/tensorflow/blob/master/tensorflow/lite/examples/android/app/download-models.gradle)
