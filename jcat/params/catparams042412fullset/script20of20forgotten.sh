#!/bin/bash
ant run -Dparams=params/catparams042412fullset/cat042312_prior1000_info1pt0_noise1pt0_mean120_stdev50.params
cp -r ./log042412fullset/* ~/Dropbox/logs/log042412fullset/
mkdir ~/Dropbox/logs/log042412fullset/completelog/cat042312_prior1000_info1pt0_noise1pt0_mean120_stdev50
cp -r ./log/* ~/Dropbox/logs/log042412fullset/completelog/cat042312_prior1000_info1pt0_noise1pt0_mean120_stdev50