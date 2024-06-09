# FRAMEWORK

# IMPLEMENTATION
## Survival
Stronghold areas are stellar areas that can be conquered by factions. To conquer one, conquer all of its ControlZones (CZ).
 
Allies owning CZs count into your CZs too. If you and your allies dont hold any CZs anymore, the stronghold automatically goes to the faction (except NEUTRAL) that ownes the most territory (ignoring alliances).

 - CZs have to be conquered in an order by the owning faction, defined by their indices (0->1->...->n).
 - An attacker must conquer in the reverse order  (n->...->0). 
 - CZs can only be conquered if their index is next in line to be conquered.
 - After each successful conquering, all CZs can't be conquered for a timeout. 