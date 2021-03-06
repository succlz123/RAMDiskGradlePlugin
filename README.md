## RAM Disk plugin for Gradle

A Gradle plugin that can help developer to use RAM disk to speed up compilation in an easy way.

## Usage

~~~
repositories {
    maven { url 'https://jitpack.io' }
}
~~~

~~~
dependencies {
    classpath 'com.github.succlz123:ramdisk:0.0.1'
}
~~~

**Just need to apply plugin in the app module once**
~~~
apply plugin: 'org.succlz123.ramdisk'
~~~

### Windows

For Windows platform, there is no convenient way to use system tool to create a RAM disk, so please download the imdisk to work with it.

https://sourceforge.net/projects/imdisk-toolkit/

## Option

gradle.properties

~~~
# if your RAM is smaller than 16GB, you need to enable the plugin manually
RAMDisk.enable=true
RAMDisk.name=RAMDiskForGradle
# 1024MB = 1GB
RAMDisk.size=1024
# apfs or hfs+, defalut is apfs
RAMDisk.mac.format=apfs
~~~

## Other

if you encouter some stranger issues with this plugin, please clean the build cache and delete the .gradle dir which locate in the project root path.

## ToDo

> Crazy Mode

Move the Gradle .cache dir into RAM disk.