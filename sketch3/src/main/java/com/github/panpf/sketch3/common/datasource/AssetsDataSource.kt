/*
 * Copyright (C) 2019 panpf <panpfpanpf@outlook.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.panpf.sketch3.common.datasource

import android.content.Context
import com.github.panpf.sketch3.common.DataFrom
import com.github.panpf.sketch3.util.MD5Utils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class AssetsDataSource(
    private val context: Context,
    private val assetsFilePath: String
) : DataSource {

    override val from: DataFrom
        get() = DataFrom.LOCAL

    @get:Throws(IOException::class)
    @get:Synchronized
    override val length: Long by lazy {
        context.assets.openFd(assetsFilePath).use {
            it.length
        }
    }

    @Throws(IOException::class)
    override fun newInputStream(): InputStream {
        return context.assets.open(assetsFilePath)
    }

    @Throws(IOException::class)
    override fun getFile(outDir: File?, outName: String?): File? {
        if (outDir == null || (!outDir.exists() && !outDir.parentFile.mkdirs())) {
            return null
        }

        val outFile = File(outDir, outName ?: MD5Utils.md5(assetsFilePath))
        newInputStream().use { inputStream ->
            FileOutputStream(outFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return outFile
    }

    override fun toString(): String {
        return "AssetsDataSource(from=$from, assetsFilePath='$assetsFilePath')"
    }
}