# super-resolution-service
* Perform 4x upscaling using ESRGAN
  * ONNX runtime to perform inferencing
  * ESRGAN `.onnx` converted from the original PyTorch model
* Compile and test locally with `./gradlew build`
* Build and deploy container to CUDA enabled Docker host with:
  ```
  docker build -t super-resolution-service .
  docker run -p 8080:8080 --name super-resolution-service --gpus all super-resolution-service
  ```

## TODO
* add more models