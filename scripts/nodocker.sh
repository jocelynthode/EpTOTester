#!/bin/bash	

if [ -z "$1" ]
  then
    echo "No argument supplied"
    exit
fi

addtime() {
    while IFS= read -r line; do
        echo "$(date +%s%N | cut -b1-13) $line"
    done
}

cd ..; ./gradlew --daemon clean shadowJar && cd scripts/;
# wait for all peers
sleep 30



MY_IP_ADDR=$(ifconfig eth0 | grep "inet addr" | cut -d ':' -f 2 | cut -d ' ' -f 1)

number_of_peers=$1
last_port=$((10000+number_of_peers))
ips=(172.16.0.42: 172.16.0.43: 172.16.0.48: 172.16.0.49:)
own_ip="$MY_IP_ADDR:"
ports=()
for ((p=10000; p<last_port; p++));do 
	ports+=($p)
done
args=()
for f1 in {0..3}
do
	for ((f2=0; f2<number_of_peers; f2++));do
		args+=(${ips[$f1]}${ports[$f2]})
	done
done

for ((i=0; i<number_of_peers;i++));do	
	t_args=()
	complete_ip="$own_ip${ports[$i]}"
	for t in "${args[@]}"
	do
		if [ "$t" != "$complete_ip" ]; then
			t_args+=("$t")
		fi
	done
	final_args=("$complete_ip")
	for m in "${t_args[@]}"
	do
		final_args+=("$m")
	done
	final_args_string=""

	for n in "${final_args[@]}"
	do
		final_args_string+="$n "
	done
	#echo $final_args_string
	echo 'Starting epto peer'
	exec java -Xms20m -Xmx50m -cp ../build/libs/epto-1.0-SNAPSHOT-all.jar epto.utilities.App $final_args_string | addtime > localhost$i.txt 2>&1 &
done
