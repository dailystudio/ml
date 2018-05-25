#!/bin/sh

echo "deploying model to mobile..."
adb shell mkdir /sdcard/deeplab/
adb push frozen_inference_graph.pb /sdcard/deeplab/
