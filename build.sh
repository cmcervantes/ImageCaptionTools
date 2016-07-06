#!/usr/bin/env bash
#@author ccervantes

read -d '' usage <<"EOF"
==========build.sh==========
Builds the current package, using maven
-h | --help     Prints this usage and exits
--online        Downloads the maven repos (default:false)
EOF

#### Argument parsing stolen from the very helpful
#### http://stackoverflow.com/questions/192249/how-do-i-parse-command-line-arguments-in-bash
for i in "$@"
    do
    case $i in
        -h|--help)
        HELP=true
        shift # past argument=value
        ;;
        --online)
        ONLINE=true
        shift # past argument with no value
        ;;
        #-o=*|--option=*)
        #OPTIONVAL="${i#*=}"
        #shift # past argument=value
        #;;
        *)
         # unknown option
        ;;
    esac
done
if [ "$HELP" = true ]; then
    echo "$usage"
    exit
fi

if [ "$ONLINE" = true ]; then
    mvn clean package > build.log
else
    mvn --offline clean package > build.log
fi

grep "^\[ERROR\]" build.log