#!/usr/bin/env bash

scp -r ~/source/ImageCaptionTools/src/ clgrad2:~/source/ImageCaptionTools/ >/dev/null
scp ~/source/ImageCaptionTools/pom.xml clgrad2:~/source/ImageCaptionTools/ >/dev/null
scp ~/source/ImageCaptionTools/build.sh clgrad2:~/source/ImageCaptionTools/ >/dev/null
scp ~/source/ImageCaptionTools/README clgrad2:~/source/ImageCaptionTools/ >/dev/null