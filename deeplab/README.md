# DeepLab on Android
DeepLab is a state-of-art deep learning model for semantic image segmentation, where the goal is to assign semantic labels (e.g., person, dog, cat and so on) to every pixel in the input image. Here is mobile version running on Android devices.


## Preparing the models

Before running the demo on Android device, you need to prepare a DeepLab inference model for mobile device.

1. Clone the DeepLab model source code from TensorFlow modal repository:
https://github.com/tensorflow/models/tree/master/research/deeplab
```
git clone https://github.com/tensorflow/models.git
```
2. Enter your repository directory and download pretrained checkpoints and graph from <a href='g3doc/model_zoo.md'>here</a>. Models are pretrained several datasets, including PASCAL VOC 2012,  Cityscapes, and  ADE20K. Download **MobileNet-v2** based models, like mobilenetv2_coco_voc_trainaug. Because it has smaller size and better performance. **Xception_65** based models cannot be loaded by TensorFlow Mobile inference engine. After the model is downloaded, unzip and put the files into directory model under **research/deeplab/**,  like this:
```
$ cd models/
$ cd research/deeplab/
$ mkdir -p model
$ (... unzip the downloaded model file.. )
$ ls -1 model/
frozen_inference_graph.pb
model.ckpt-30000.data-00000-of-00001
model.ckpt-30000.index
```

3. Modify one line of code of export model scriptto generate model for Tensorflow Mobile.
```
vim export_model.py
```
Modify **line 131** from
```python
 semantic_predictions = tf.slice(
         predictions[common.OUTPUT_TYPE],
         [0, 0, 0],
         [1, resized_image_size[0], resized_image_size[1]])
```
to
```python
 semantic_predictions = tf.slice(
         tf.cast(predictions[common.OUTPUT_TYPE], tf.int32),
         [0, 0, 0],
         [1, resized_image_size[0], resized_image_size[1]])
```
Changing this line is casting the INT64 Sclie Operation to INT32 Slice Operation. INT64 Sclie Operation is NOT supported on Android currently and it will cause a runtime exception during inference.
 
4. Export model with the command:
``` bash
python export_model.py \
    --checkpoint_path model/model.ckpt-30000 \
    --export_path ./frozen_inference_graph.pb \
    --model_variant="mobilenet_v2" \
    --num_classes=21 \
    --crop_size=513 \
    --crop_size=513 \
    --inference_scales=1.0
```
A new generated frozen graph for TensorFlow Mobile is under current directory.


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