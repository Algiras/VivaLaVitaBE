#!/usr/bin/env bash

sbt assembly
scp target/scala-2.12/VivaLaVita-assembly-0.0.1-SNAPSHOT.jar ${DEPLOY_SERVER}:/home/root/
ssh -t ${DEPLOY_SERVER} "systemctl stop vivaLaVita && systemctl start vivaLaVita && sleep 5 && systemctl status vivaLaVita"