#!/bin/bash

# Verifica se ao menos um país foi passado como argumento
if [ "$#" -lt 1 ]; then
    echo "Uso: $0 <pais1> <pais2> ... <paisN>"
    exit 1
fi

# URL base do GADM
# https://geodata.ucdavis.edu/gadm/gadm4.1/json/gadm41_ARG_0.json
BASE_URL="https://geodata.ucdavis.edu/gadm/gadm4.1/json/gadm41_"

# Loop sobre cada país passado como argumento
for VAR_ONE_SELECTED_COUNTRY in "$@"; do
    # Faz a requisição para obter os links GeoJSON
    echo "Buscando arquivos GeoJSON para o país: $VAR_ONE_SELECTED_COUNTRY"

    FILE_LEVEL_0_LINK="${BASE_URL}${VAR_ONE_SELECTED_COUNTRY}_0.json"
    FILE_LEVEL_1_LINK="${BASE_URL}${VAR_ONE_SELECTED_COUNTRY}_1.json"
    FILE_LEVEL_2_LINK="${BASE_URL}${VAR_ONE_SELECTED_COUNTRY}_2.json"
    FILE_LEVEL_3_LINK="${BASE_URL}${VAR_ONE_SELECTED_COUNTRY}_3.json"
    FILE_LEVEL_4_LINK="${BASE_URL}${VAR_ONE_SELECTED_COUNTRY}_4.json"

    echo "=====> Baixando: $FILE_LEVEL_0_LINK"
    curl -O "$FILE_LEVEL_0_LINK"

    echo "=====> Baixando: $FILE_LEVEL_1_LINK"
    curl -O "$FILE_LEVEL_1_LINK"

    echo "=====> Baixando: $FILE_LEVEL_2_LINK"
    curl -O "$FILE_LEVEL_2_LINK"

    echo "=====> Baixando: $FILE_LEVEL_3_LINK"
    curl -O "$FILE_LEVEL_3_LINK"

    echo "=====> Baixando: $FILE_LEVEL_4_LINK"
    curl -O "$FILE_LEVEL_4_LINK"

done

echo "Download concluído."
