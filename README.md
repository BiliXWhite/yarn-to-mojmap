# yarn-to-mojmap
这个工具可以生成适用于 proprecessor 的从 Yarn 映射到 Mojmap 映射


## 使用方法

1. 从 [Releases](https://github.com/BiliXWhite/yarn-to-mojmap/releases) 页面下载最新版本的工具。
2. 解压下载的文件到一个目录。
3. 打开命令行工具，导航到解压后的目录。
4. 运行以下命令来使用工具：
   ```sh
   java -jar yarn-to-mojmap.jar -m 1.19.2 -y 10 -f tiny
   ```
   其中 `-m` 指定 Minecraft 版本，`-y` 指定 Yarn 构建号，`-f` 指定输出格式。

## 添加的特性
- 中文翻译
- 即开即用
- 自动生成文件而不是需要你手动更改输出

## 构建

1. 克隆仓库到本地：
   ```sh
   git clone https://github.com/BiliXWhite/yarn-to-mojmap.git
   cd yarn-to-mojmap
   ```
2. 确保已安装 JDK 17 或更高版本，并设置好环境变量。
3. 使用 Gradle 构建项目：
   ```sh
   ./gradlew build
   ```
4. 运行主程序，例如：
   ```sh
   ./gradlew run --args="-m 1.19.2 -y 10 -f tiny"
   ```