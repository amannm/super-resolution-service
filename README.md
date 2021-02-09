# super-resolution-service
* Perform 4x upscaling using ESRGAN
    * ONNX runtime to perform inferencing
    * ESRGAN `.onnx` converted from the original PyTorch model
* Compile with `./gradlew build`

## TODO
* add API endpoint
* dockerize
* add CUDA/GPU supported build task
* move model specific variables into a JSON config file
* add more models