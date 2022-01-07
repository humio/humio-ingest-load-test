#!/bin/bash

declare -r perf_simulation=${PERF_SIMULATION:-HECSimulation}

env_var_arg() {
  local -r env_var=$1
  local -r property=$2

  local -r env_var_value="$(eval "echo \$$env_var")"
  if [ ! -z "$env_var_value" ]; then
    echo "-D${property}=${env_var_value}"
  fi
}

common_args() {
  env_var_arg HUMIO_BASE_URL baseurls
  env_var_arg HUMIO_TOKEN token
  env_var_arg PERF_TIME time
}

hec_simulation_args() {
  common_args
  env_var_arg PERF_BULK_SIZE bulksize
  env_var_arg PERF_DATASPACES dataspaces
  env_var_arg PERF_EVENT_SIZE eventsize
  env_var_arg PERF_DATASOURCES datasources
  env_var_arg PERF_FIELDS fields
  env_var_arg PERF_USERS users
  env_var_arg RANDOMNESS randomness 
}

hec_template_simulation_args() {
  common_args
  env_var_arg PERF_BULK_SIZE bulksize
  env_var_arg PERF_DATASPACES dataspaces
  env_var_arg PERF_DATASOURCES datasources
  env_var_arg PERF_USERS users
  env_var_arg TEMPLATE template
}

fixed_rate_ingest_simulation() {
  common_args
  env_var_arg PERF_TENS_GB_PER_DAY tensGbPerDay
}

query_simulation() {
  common_args
  env_var_arg PERF_SEARCH_QUERY searchQuery
  env_var_arg PERF_SEARCH_DURATION searchDuration
}

case $perf_simulation in
  "HECSimulation")
    cmd_args=$(hec_simulation_args)
  ;;
  "HECTemplateSimulation")
    cmd_args=$(hec_template_simulation_args)
  ;;
  "HECRandomnessSimulation")
    cmd_args=$(hec_simulation_args)
  ;;
  "FilebeatSimulation")
    # Filebeat shares the same args as HECSimulation
    cmd_args=$(hec_simulation_args)
  ;;
  "FixedRateIngestSimulation")
    cmd_args=$(fixed_rate_ingest_simulation)
  ;;
  "QuerySimulation")
    cmd_args=$(query_simulation)
  ;;
  *)
    echo "Simulation not supported"
    exit 1
  ;;
esac

java -jar $cmd_args perftest.jar -s com.humio.perftest.${perf_simulation}
