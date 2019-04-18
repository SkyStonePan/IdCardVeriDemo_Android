IdcardVeri2.0 的 AndroidDemo

一、环境要求 

1. 运行环境 armeabi-v7a

2. 系统要求 Android 4.4 (API Level 19)及以上

3. 开发环境 Android Studio

二、快速上手

1. 在src->main目录下新建文件夹jniLibs->armeabi-v7a，将libarcsoft_face.so和libarcsoft_idcardveri.so添加到src->main->jniLibs->armeabi-v7a路径下

2. 在app目录下新建文件夹libs，将arcsoft_face.jar放入app->libs路径下，并依赖进工程

3. 将官网申请sdk获取到的APP_ID和SDK_KEY填入Constants.java

4. 完成身份证阅读器代码的实现

5. 连接身份证阅读器

6. 运行app

三、问题指南 

1. 第一次使用设备需要联网激活

2. 本Demo暂不提供身份证阅读器的实现，用户需自己实现（用户也可修改代码中SAMPLE_NAME及SAMPLE_FACE的值，点击“模拟身份证输入”进行模拟人证识别）

3. 详细接入指南可见官网：https://ai.arcsoft.com.cn/manual/idcard_android_guide_v2.html；

4. 常见问题可见SDK中的doc文档ARCSOFT_IDCARDVERI_DEVELOPER'S_GUIDE.pdf，或官网帮助与支持。