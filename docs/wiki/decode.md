# Decoder

Translations: [简体中文](decode_zh)

[Decoder] is used to decode image files. It has the following implementation:

* [BitmapFactoryDecoder][BitmapFactoryDecoder]：Decode images using Android's
  built-in [BitmapFactory], which is the last resort decoder
* [DrawableDecoder][DrawableDecoder]：Decode vector, shape and other xml drawable images supported by
  Android
* [SvgDecoder][SvgDecoder]：Decode static svg files using the [BigBadaboom]/[androidsvg]
  library ([Learn More](svg.md))
* [ApkIconDecoder][ApkIconDecoder]：Decode the icon of the Apk
  file ([Learn more](apk_app_icon.md#displays-an-icon-for-the-apk-file))
* [GifAnimatedDecoder][GifAnimatedDecoder]：Decode gifs using Android's
  built-in [ImageDecoder] ([Learn more](animated_image.md))
* [GifDrawableDecoder][GifDrawableDecoder]：Decoding gif images using [GifDrawable] from
  the [koral--]/[android-gif-drawable] library ([Learn more](animated_image.md))
* [GifMovieDecoder][GifMovieDecoder]：Decode gif images using Android's
  built-in [Movie] ([Learn more](animated_image.md))
* [HeifAnimatedDecoder][HeifAnimatedDecoder]：Use Android's built-in [ImageDecoder] to decode heif
  animations ([Learn More](animated_image.md))
* [WebpAnimatedDecoder][WebpAnimatedDecoder]：Use Android's built-in [ImageDecoder] to decode webp
  animations ([Learn more](animated_image.md))
* [VideoFrameDecoder][VideoFrameDecoder]：Decoding frames of video files using Android's
  built-in [MediaMetadataRetriever] class ([Learn more](video_frame.md))
* [FFmpegVideoFrameDecoder][FFmpegVideoFrameDecoder]：Decode frames of a video file using
  the [FFmpegMediaMetadataRetriever] class of the [wseemann]/[FFmpegMediaMetadataRetriever-project]
  library ([Learn more](video_frame.md))

When decoding is required, Sketch will traverse the [Decoder] list to find a [Decoder] that can
decode the current image, and then execute its decode method to obtain the decoding result.

### Register Decoder

By default, Sketch only registers [DrawableDecoder] and [BitmapFactoryDecoder]. Other [Decoder] need
to be registered manually according to your needs, as follows:

```kotlin
/* Register for all ImageRequest */
class MyApplication : Application(), SingletonSketch.Factory {

    override fun createSketch(): Sketch {
        return Sketch.Builder(this).apply {
            components {
                addDecoder(MyDecoder.Factory())
            }
        }.build()
    }
}

/* Register for a single ImageRequest */
imageView.displayImage("asset://sample.mypng") {
    components {
        addDecoder(MyDecoder.Factory())
    }
}
```

### Extend New Decoder

1.First, you need to implement the [Decoder] interface to implement your [Decoder] and its Factory,
as follows:

```kotlin
class MyDecoder : Decoder {

    override suspend fun decode(): Result<BitmapDecodeResult> {
        // Decode image here
    }

    companion object {
        const val MY_MIME_TYPE = "image/mypng"
    }

    class Factory : Decoder.Factory {

        override fun create(
            sketch: Sketch,
            requestContext: RequestContext,
            fetchResult: FetchResult
        ): Decoder? {
            val mimeType = fetchResult.mimeType
            val dataSource = fetchResult.dataSource
            // 在这通过 mimeType 或 dataSource 判断当前图像是否是
            // The target type of MyDecoder, if yes, returns a new MyDecoder
            return if (fetchResult.mimeType == MY_MIME_TYPE) {
                MyDecoder()
            } else {
                null
            }
        }
    }
}
```

2.Then refer to [Register Decoder](#Register-Decoder) to register it

> Caution:
> 1. Customizing [Decoder] requires applying many properties related to image quality and size in
     ImageRequest, such as bitmapConfig, resize, colorSpace, etc. You can refer to other [Decoder]
     implementations
> 2. If your [Decoder] is decoding animated images, you must determine the [ImageRequest]
     .disallowAnimatedImage parameter.




Sketch's decoding process supports interceptors, which allow you to change the input and output
before and after decoding

First, implement the [DecodeInterceptor] interface to implement your [DecodeInterceptor], as
follows:

```kotlin
class MyDecodeInterceptor : DecodeInterceptor {

    // If the current DecodeInterceptor modifies the returned result and is only used for partial requests, 
    // then give a distinct key to build the cache key, otherwise null is fine
    override val key: String = "MyDecodeInterceptor"

    // Used for sorting, the higher the value, the lower it is in the list. The value range is 0 ~ 100. 
    // Usually zero. Only EngineDecodeInterceptor can be 100
    override val sortWeight: Int = 0

    @WorkerThread
    override suspend fun intercept(
        chain: DecodeInterceptor.Chain,
    ): Result<DecodeResult> {
        val newRequest = chain.request.newRequest {
            bitmapConfig(Bitmap.Config.ARGB_4444)
        }
        return chain.proceed(newRequest)
    }
}
```

> 1. MyDecodeInterceptor demonstrates an example of changing the Bitmap.Config for all
     requests to ARGB_4444
> 2. If you want to modify the result, just intercept the result returned by the proceed method and
     return a new [DecodeResult]
> 3. If you want to stop executing requests, just don't execute the proceed method

Then, register your DecodeInterceptor via the addDecodeInterceptor() and
addDrawableDecodeInterceptor() methods as follows:

```kotlin
/* Register for all ImageRequests */
class MyApplication : Application(), SingletonSketch.Factory {

    override fun createSketch(): Sketch {
        return Sketch.Builder(this).apply {
            components {
                addDecodeInterceptor(MyDecodeInterceptor())
            }
        }.build()
    }
}

/* Register for a single ImageRequest */
imageView.displayImage("file:///sdcard/sample.mp4") {
    components {
        addDecodeInterceptor(MyDecodeInterceptor())
    }
}
```

[comment]: <> (class)

[Decoder]: ../../sketch-core/src/commonMain/kotlin/com/github/panpf/sketch/decode/Decoder.kt

[Image]: ../../sketch-core/src/commonMain/kotlin/com/github/panpf/sketch/Image.kt

[FetchResult]: ../../sketch-core/src/commonMain/kotlin/com/github/panpf/sketch/fetch/FetchResult.kt

[BitmapFactoryDecoder]: ../../sketch-core/src/commonMain/kotlin/com/github/panpf/sketch/decode/internal/BitmapFactoryDecoder.kt

[DrawableDecoder]: ../../sketch-core/src/commonMain/kotlin/com/github/panpf/sketch/decode/internal/DrawableDecoder.kt

[FFmpegVideoFrameDecoder]: ../../sketch-video-ffmpeg/src/main/kotlin/com/github/panpf/sketch/decode/FFmpegVideoFrameDecoder.kt

[ApkIconDecoder]: ../../sketch-extensions-core/src/main/kotlin/com/github/panpf/sketch/decode/ApkIconDecoder.kt

[VideoFrameDecoder]: ../../sketch-video/src/main/kotlin/com/github/panpf/sketch/decode/VideoFrameDecoder.kt

[SvgDecoder]: ../../sketch-svg/src/main/kotlin/com/github/panpf/sketch/decode/SvgDecoder.kt

[DrawableDecoder]: ../../sketch-core/src/commonMain/kotlin/com/github/panpf/sketch/decode/internal/DrawableDecoder.kt

[GifAnimatedDecoder]: ../../sketch-animated/src/main/kotlin/com/github/panpf/sketch/decode/GifAnimatedDecoder.kt

[HeifAnimatedDecoder]: ../../sketch-animated/src/main/kotlin/com/github/panpf/sketch/decode/HeifAnimatedDecoder.kt

[WebpAnimatedDecoder]: ../../sketch-animated/src/main/kotlin/com/github/panpf/sketch/decode/WebpAnimatedDecoder.kt

[GifDrawableDecoder]: ../../sketch-animated-koralgif/src/main/kotlin/com/github/panpf/sketch/decode/GifDrawableDecoder.kt

[GifMovieDecoder]: ../../sketch-animated/src/main/kotlin/com/github/panpf/sketch/decode/GifMovieDecoder.kt

[ImageRequest]: ../../sketch-core/src/commonMain/kotlin/com/github/panpf/sketch/request/ImageRequest.kt

[wseemann]: https://github.com/wseemann

[FFmpegMediaMetadataRetriever-project]: https://github.com/wseemann/FFmpegMediaMetadataRetriever

[FFmpegMediaMetadataRetriever]: https://github.com/wseemann/FFmpegMediaMetadataRetriever/blob/master/core/src/main/kotlin/wseemann/media/FFmpegMediaMetadataRetriever.java

[BigBadaboom]: https://github.com/BigBadaboom

[androidsvg]: https://github.com/BigBadaboom/androidsvg

[koral--]: https://github.com/koral--

[android-gif-drawable]: https://github.com/koral--/android-gif-drawable

[GifDrawable]: https://github.com/koral--/android-gif-drawable/blob/dev/android-gif-drawable/src/main/kotlin/pl/droidsonroids/gif/GifDrawable.java

[Movie]: https://cs.android.com/android/platform/superproject/+/master:frameworks/base/graphics/kotlin/android/graphics/Movie.java

[ImageDecoder]: https://cs.android.com/android/platform/superproject/+/master:frameworks/base/graphics/kotlin/android/graphics/ImageDecoder.java

[BitmapFactory]: https://cs.android.com/android/platform/superproject/+/master:frameworks/base/graphics/kotlin/android/graphics/BitmapFactory.java

[MediaMetadataRetriever]: https://cs.android.com/android/platform/superproject/+/master:frameworks/base/media/kotlin/android/media/MediaMetadataRetriever.java

[DecodeInterceptor]: ../../sketch-core/src/commonMain/kotlin/com/github/panpf/sketch/decode/DecodeInterceptor.kt

[DecodeResult]: ../../sketch-core/src/commonMain/kotlin/com/github/panpf/sketch/decode/DecodeResult.kt

[ImageRequest]: ../../sketch-core/src/commonMain/kotlin/com/github/panpf/sketch/request/ImageRequest.kt