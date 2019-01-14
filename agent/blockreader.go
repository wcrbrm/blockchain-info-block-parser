package main

import (
	"encoding/json"
	"fmt"
	"net/http"
	"os"
	"time"
)

/*{
    "hash":"0000000000000538200a48202ca6340e983646ca088c7618ae82d68e0c76ef5a",
    "time":1325794737,
    "block_index":841841,
    "height":160778,
    "txIndexes":[13950369,13950510,13951472]
}*/

type LatestBlockStruct struct {
	Time       int    `json:"time"`
	BlockIndex int    `json:"block_index"`
	Height     int    `json:"height"`
	Hash       string `json:"hash"`
}

func getLatestBlockHeight() (obj *LatestBlockStruct, erro error) {
	url := "https://blockchain.info/latestblock"
	timeout := time.Duration(5 * time.Second)
	client := http.Client{Timeout: timeout}
	response, err := client.Get(url)
	if err != nil {
		return nil, err
	}
	defer response.Body.Close()

	var res LatestBlockStruct
	errDecode := json.NewDecoder(response.Body).Decode(&res)
	if errDecode != nil {
		return nil, errDecode
	}
	return &res, nil
}

func main() {
	res, err := getLatestBlockHeight()
	if err != nil {
		fmt.Println("ERROR:", err)
		os.Exit(1)
	}
	fmt.Println("Time:", res.Time)
	fmt.Println("BlockIndex:", res.BlockIndex)
	fmt.Println("Height:", res.Height)
	fmt.Println("Hash:", res.Hash)
}
