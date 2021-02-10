# super-resolution-service
* Perform 4x upscaling using ESRGAN
    * ONNX runtime to perform inferencing
    * ESRGAN `.onnx` converted from the original PyTorch model
* Compile with `./gradlew build`

## TODO
* dockerize
* add CUDA/GPU supported build task
* add more models