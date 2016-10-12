/*
Copyright 2016 Joel Whittaker-Smith

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.joelws.simple.poller.listener

import com.github.drapostolos.rdp4j.DirectoryListener
import com.github.drapostolos.rdp4j.FileAddedEvent
import com.github.drapostolos.rdp4j.FileModifiedEvent
import com.github.drapostolos.rdp4j.FileRemovedEvent
import com.joelws.simple.poller.SftpOperation
import com.joelws.simple.poller.handler.UnzipHandler
import kotlinx.coroutines.async
import org.slf4j.LoggerFactory

class ZipListener(private val sftpOperation: SftpOperation, private val workingDir: String, private val handler: UnzipHandler) : DirectoryListener {

    companion object {
        const val TEMP_DIR = "tmp"
        private val logger = LoggerFactory.getLogger(ZipListener::class.java)
    }

    override fun fileModified(event: FileModifiedEvent) {
        logger.info(event.fileElement.name)
    }

    override fun fileRemoved(event: FileRemovedEvent) {
        logger.info("File removed: ${event.fileElement.name}")
    }

    override fun fileAdded(event: FileAddedEvent) {

        val fileName = event.fileElement.name
        logger.info("File added: $fileName")

        val absoluteName = "$workingDir/$fileName"

        val tempDirFileName = "$TEMP_DIR/$fileName"

        async<Unit> {
            logger.info("Starting download of $fileName to directory[$TEMP_DIR]")

            await(sftpOperation.get(absoluteName, tempDirFileName))

            logger.info("Downloading of $fileName is complete")
            handler.execute(tempDirFileName)
        }
                .exceptionally { throwable ->
                    logger.error("Download error: ", throwable.cause)
                    throw throwable
                }.get()


    }

}
