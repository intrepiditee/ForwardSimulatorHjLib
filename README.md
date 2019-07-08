# A Forward Simulator for IBD Sharing

## 1. Simulation

  #### 1.Usage
   
  ```bash
  bash run.sh --simulate numberOfGenerations firstGenerationToStore lastGenerationToStore generationSize numberOfThreads
  ```
        
  This will simulate for "numOfGeneration" generations including the founder generation.
  Each generation has "generationSize" individuals, of which half are males and the other half are females.
  It will store the genomes, mutated sites, and parent IDs of individuals
  from Generation "firstGenerationToStore" to Generation "lastGenerationToStore" (starting from zero).
  Genomes and mutated sites are stored in binary for further processing. 
  "numberOfThreads" is the maximum number of threads that will be spawned during the execution.
    
  __Example:__
    
  ```bash
  bash run.sh --simulate 10 6 9 1000 22
  ```
  This will simulate for 10 generations each of 1000 individuals using 22 threads.
  The last 4 generations (6, 7, 8, 9) will be stored.
    
    
  #### 2. File Output
  
  All the genomes, mutations sites, and parent IDs will be generated in ForwardSimulatorHjLib/target/out in binary.
  These files are required for computing pairwise distances and generating VCF files.
  
  All the IBD segments will be generated in ForwardSimulatorHjLib/target/ibd in text.
  Each file is tab delimited. The first line of each file is a header. 

## 2. Subsetting UK Biobank VCF Files

  ___This must be done before generating VCF files from the simulation.___
  
  Alternatively, you can use VCFtools to do this.
  Just make sure input files are in the correct positions.
  
  #### 1.Usage
    
  __Example:__
  
  #### 2. File Output
    
## 3. Generating VCF Files from Simulation

  #### 1.Usage
    
  __Example:__
    
  #### 2. File Output
  
## 4. Generating Mapping Files for RaPID

  #### 1.Usage
    
  __Example:__
    
  #### 2. File Output
 
## 5. Computing Pairwise Distances

  #### 1.Usage
    
  __Example:__
    
  #### 2. File Output