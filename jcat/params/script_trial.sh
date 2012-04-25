#!/bin/bash
ant run -Dparams=params/catparams042412fullset/cat042312_prior10_infopt001_noisept001_mean60_stdev10.params
cp -r ~/makers-market/jcat/log042412fullset/ ~/Dropbox/logs/log042412fullset/
cp -r ~/makers-market/jcat/log/ ~/Dropbox/logs/log042412fullset/completelog/cat042312_prior10_infopt001_noisept001_mean60_stdev10