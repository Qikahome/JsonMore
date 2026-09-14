kill @e[type=minecraft:item,tag=jm_test_jukebox]
kill @e[type=minecraft:item,tag=jm_test_value]
summon minecraft:item 0 100 0 {Item:{id:"testpack:test_components_jukebox",count:1},Tags:["jm_test_jukebox"]}
summon minecraft:item 0 100 0 {Item:{id:"testpack:test_components",count:1},Tags:["jm_test_value"]}
data get entity @e[type=minecraft:item,tag=jm_test_jukebox,limit=1] Item
data get entity @e[type=minecraft:item,tag=jm_test_value,limit=1] Item
