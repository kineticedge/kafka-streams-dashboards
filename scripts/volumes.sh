#!/bin/bash


for d in `docker ps | awk '{print $1}' | tail -n +2`; do
    d_name=`docker inspect -f {{.Name}} $d`
    echo ""
    echo "$d_name ($d)"

	VOLUME_IDS=$(docker inspect -f "{{.Config.Volumes}}" $d)
	VOLUME_IDS=$(echo ${VOLUME_IDS} | sed 's/map\[//' | sed 's/]//')
	
	array=(${VOLUME_IDS// / })
	for i in "${!array[@]}"
	do
		VOLUME_ID=$(echo ${array[i]} | sed 's/:{}//')
		VOLUME_SIZE=$(docker exec -ti $d_name du -d 0 -h ${VOLUME_ID})
	    echo "    $VOLUME_SIZE"
	done

done

