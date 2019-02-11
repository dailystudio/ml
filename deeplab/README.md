# DeepLab on Android
DeepLab is a state-of-art deep learning model for semantic image segmentation, where the goal is to assign semantic labels (e.g., person, dog, cat and so on) to every pixel in the input image. Here is mobile version running on Android devices.

# Latest updates
Tensorflow Lite annouced a preview version with GPU support, you can read [TensorFlow Lite GPU Delegate Tutorial](https://www.tensorflow.org/lite/performance/gpu) for further information.

Along with this preview release, it also published a set of pre-trained models for testing the performance. These models include a DeepLab tflite model.

Due to Tensorflow mobile is deprecated. The latest code of this repository will use Tensorflow Lite instead of Tensorflow Mobile. The following parts of this document will explain a bit more about this new Tensorflow Lite version. Tensorflow Mobile related content is archived [here](doc/README_OLD.md).

## Downloading the TFlite model

According to the [TensorFlow Lite GPU Delegate Tutorial](https://www.tensorflow.org/lite/performance/gpu), with the release of the GPU delegate, they included a handful of models that can be run on the backend. You can download the DeepLab segmentation model which supports 257 x 257 inputs.

Here is a download shortcut:
[DeepLab segmentation (257x257)](https://storage.googleapis.com/download.tensorflow.org/models/tflite/gpu/deeplabv3_257_mv_gpu.tflite)

Don't worry, if you cannot download the original one from the link above, I have already included one the source codes. It is placed under app/src/main/assets/

# Running the demo

1. Push the model to the device:
```bash
adb shell mkdir /sdcard/deeplab/
adb push frozen_inference_graph.pb /sdcard/deeplab/
```
Anyway, the final inference model should be here:
> /sdcard/deeplab/frozen_inference_graph.pb

2. Run the demo, here is demo screen recording.
<img src=".github/deeplab_demo.gif" width="280" height="498" alt="DeepLab Demo"/>

## License

[Apache License 2.0](LICENSE)

## Attributions/Thanks
- DeepLabv3+:
```
@article{deeplabv3plus2018,
  title={Encoder-Decoder with Atrous Separable Convolution for Semantic Image Segmentation},
  author={Liang-Chieh Chen and Yukun Zhu and George Papandreou and Florian Schroff and Hartwig Adam},
  journal={arXiv:1802.02611},
  year={2018}
}
```

- MobileNetv2:

```
@inproceedings{mobilenetv22018,
  title={Inverted Residuals and Linear Bottlenecks: Mobile Networks for Classification, Detection and Segmentation},
  author={Mark Sandler and Andrew Howard and Menglong Zhu and Andrey Zhmoginov and Liang-Chieh Chen},
  booktitle={CVPR},
  year={2018}
}
```
- Without the advice given by [Liang-Chieh Chen](https://github.com/aquariusjay), we cannot successfully export the model on mobile devices.