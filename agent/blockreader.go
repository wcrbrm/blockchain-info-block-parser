package main

import (
	"encoding/json"
	"fmt"
	"net/http"
	"os"
	"time"
)

type ErrorPacket struct {
	Type string `json:"packet"`
	Time int64 `json:"time"`
	Result string `json:"result"`
	Reason string `json:"error"`
}
func errorOutput(packetType string, err error) {
	if (err != nil) {
		str, _ := json.Marshal(&ErrorPacket{ packetType, time.Now().Unix(), "error", err.Error() })
		fmt.Println(string(str))
		os.Exit(1)
	}
}

type LatestBlockStruct struct {
	Time       int    `json:"time"`
	BlockIndex int    `json:"block_index"`
	Height     int    `json:"height"`
	Hash       string `json:"hash"`
}
type LatestBlockPacket struct {
	Type string `json:"packet"`
	Time int64 `json:"time"`
	Result string `json:"result"`
	Data LatestBlockStruct `json:"data"`
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
/*{
    "hash":"0000000000000538200a48202ca6340e983646ca088c7618ae82d68e0c76ef5a",
    "time":1325794737,
    "block_index":841841,
    "height":160778,
    "txIndexes":[13950369,13950510,13951472]
}*/
	errDecode := json.NewDecoder(response.Body).Decode(&res)
	if errDecode != nil {
		return nil, errDecode
	}
	return &res, nil
}

func outputLatestBlockHeight() {
	data, err := getLatestBlockHeight()
	if err != nil {
		errorOutput("latest", err)
	}
	str, _ := json.Marshal(&LatestBlockPacket{ "latest", time.Now().Unix(), "ok", *data })
	fmt.Println(string(str))
}

type TxOut struct {
	AddrTagLink string `json: "addr_tag_link,omitempty"`
	AddrTag     string `json: "addr_tag,omitempty"`
	Spent bool `json: "spent"`
	Index int64 `json: "tx_index"`
	Type  int `json: "type"`
	Addr  string `json: "addr"`
	Value int `json: "value"`
	N     int `json: "n"`
	Script  string `json: "script"`
}

type BlockTx struct {
    Time     int64 `json: "time"`
	Hash     string `json: "hash"`
    LockTime int `json: "lock_time"`
    Ver      int `json: "ver"`
    Size     int `json: "size"`
    Weight   int `json: "weight"`
    Index    int64 `json: "tx_index"`
	InSz     int `json: "vin_sz"`
	OutSz    int `json: "vout_sz"`
	Out []TxOut `json: "out"`
}
type Block struct {
    Hash string `json: "hash"`
	Ver int64 `json: "ver"`
	PrevBlock string `json: "prev_block"`
	Time int64 `json: "time"`
	ReceivedTime int64 `json: "received_time"`
	Bits int64 `json: "bits"`
	Height int64 `json: "height"`
	Nonce int64 `json: "nonce"`
	NTx int `json: "n_tx"`
	Size int64 `json: "size"`
	Index int64 `json: "block_index"`
	RelayedBy string `json: "relayed_by"`
	Tx []BlockTx `json: "tx"`
}

func getBlockByHash(hash string) (obj *Block, erro error) {
 	url := "https://blockchain.info/rawblock/hash/" + hash
 	timeout := time.Duration(20 * time.Second)
 	client := http.Client{Timeout: timeout}
	response, err := client.Get(url)
	if err != nil {
		return nil, err
	}
	defer response.Body.Close()
	errDecode := json.NewDecoder(jsonFile).Decode(&res)
	if errDecode != nil {
		return nil, errDecode
	}
    return res, nil
}

func main() {
	// outputLatestBlockHeight()
	var res Block
	jsonFile, err := os.Open("./rawblock.json")
	errorOutput("local:read", err)
	errDecode := json.NewDecoder(jsonFile).Decode(&res)
	errorOutput("json:decode", errDecode)
	fmt.Println(res.Tx)
	defer jsonFile.Close()
}
