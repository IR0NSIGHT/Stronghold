# Code structure
## core idea
- Separation into framework and effects
### stronghold framework
the stronghold framework provides a framework that creates regions (=Strongholds) that have special locations (=Strongpoints) that can be captured.
capturing these locations increase the strongholds defense counter.
the interface which effects interact through with the framework should be as minimal as possible and as abstract as possible, so changing what a stronghold is (a starsystem, a collection of star systems, just a bunch of sectors etc) remains internal and easy.
suggested interface:
- what stronghold am i in right now, what are its stats: defensepoints, strongpoint locations, owner etc.
- show borders of this (or all) strongholds: i need to know where which strongholds are.
- request to update this stronghold/position etc
- get the defensepoints of this stronghold (on top of that we can build the "invulnerable station" mechanic and other stuff too).

getStrongholdInfo(Vector3i sector): defensepoints, owner, location of strongpoints
getStrongholdBorders
requestStrongholdUpdate
getStrongholdDefensePoints
getStrongholdOwner
getStrongholdID
getStrongholdName
getStrongpoints

## effects
### voidshield
strongholds with sufficient defense points can protect their stations with a voidshield. it makes normal shields unbreakable in the whole starsystem/stronghold


# TODO
- abstract framework
  - centralized structure
    - can be updated
    - can be sent
      - instantiate
      - synch
      - delete <<
  - area is a defined stellar area
    - get abstractArea of this sector/position
    - 