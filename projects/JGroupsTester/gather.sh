mapfile -t lines < <(docker ps -a)
lines=("${lines[@]:1}")
for i in "${lines[@]}"
do
        read -a line <<<$i
        if [[ "${line[1]}" == "jgroupstester_jtester" ]]; then
                LENGTH=${#line[@]}
                LAST_POSITION=$((LENGTH - 1))
                NAME=$(basename ${line[$LAST_POSITION]})
                echo $NAME
                docker cp "${line[0]}":/code/scripts/localhost.txt ./scripts/"${NAME}"_log.txt
        fi
done

