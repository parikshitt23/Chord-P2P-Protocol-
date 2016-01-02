Project 3 – Chord


Team Members: 
Nikhil Tiware    UFID 91507670 (nikhiltiware@ufl.edu) 
Parikshit Tiwari UFID 79218564 (pariksh1tatiwari@ufl.edu) 


Working: 
We form a network of nodes entered by the user and assign keys to the nodes. After the intial network is setup five new nodes join the network. 
The incoming node is assigned a node from the existing network which is taken to be as the known node of the network. 
The finger table of the incoming node is updated and its successors and predecessors are set. 
Then the finger tables of the other nodes is updated and the keys are reassigned. 
The join of the nodes, initialization and update of the finger tables are done as per the API described in the paper. 


Routing is then implemented with each node sending out a request per second. 
The node stops sending out request when the request count hits “numRequest” entered by the user. 

The average number of hops is then counted and displayed. 


Output: 

The average number of hops is defined as total number of hops/total requests 

Largest Network Achieved: 5000 nodes 
Requests per node: 10 
Average Number of Hops: 6.05272 

The network of 5000 nodes takes a long time to run about 41 minutes. 

Fastest Network achieved: 2048 nodes
Requests per node: 10
Average Number of Hops: 5.514599609375

This network takes only 1 minutes and 5 seconds to execute.

For network size greater than 2048 setup time increases dramatically.

Execution

Navigate to the Chord folder in terminal/commadline 
for example navigate to “/home/Documents/Chord” or “C:\Desktop\Chord”

Run the command: sbt compile

Run the command: sbt “run numNodes numRequest”

where numNodes and numRequest is an integer 
example sbt “run 1000 10” to have a network of 1000 nodes and 10 request per node 
