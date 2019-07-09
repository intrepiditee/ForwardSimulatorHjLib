# A Forward Simulator for IBD Sharing

## 1. Simulation

  #### 1.Usage
   
  ```
  bash run.sh --simulate numberOfGenerations firstGenerationToStore lastGenerationToStore generationSize numberOfThreads
  ```
        
  This will simulate for "numOfGeneration" generations including the founder generation.
  Each generation has "generationSize" individuals, of which half are males and the other half are females.
  It will store the IBD segments, the genomes, mutated sites, and parent IDs of individuals
  from Generation "firstGenerationToStore" to Generation "lastGenerationToStore" (starting from zero).
  Genomes and mutated sites are stored in binary for further processing. 
  "numberOfThreads" is the maximum number of threads that will be spawned during the execution.
    
  __Example:__
    
  ```
  bash run.sh --simulate 10 6 9 1000 22
  ```
  This will simulate for 10 generations each of 1000 individuals using 22 threads.
  The last 4 generations (6, 7, 8, 9) will be stored.
    
    
  #### 2. File Output
  
  All the genomes, mutations sites, and parent IDs will be generated in ```ForwardSimulatorHjLib/target/out``` in binary.
  These files are required for computing pairwise distances and generating VCF files. They are used internally and
  are not useful to the users.
  
  All the IBD segments will be generated in ```ForwardSimulatorHjLib/target/ibd``` in text.
  Each file is tab delimited. The first line of each file is a header. 

## 2. Subsetting UK Biobank VCF Files

  ___This must be done before parsing founder sequences.___
  
  Alternatively, you can use VCFtools to do this.
  Just make sure input files comply to the requirements described in Step 3.
  
  #### 1.Usage
    
  __Example:__
  
  #### 2. File Output
  
## 3. Parsing Founder Sequences

  ___This must be done before generating VCF files from the simulation.___
  
  The input files must be in VCF format. There must be 22 files corresponding to the 22
  chromosomes. The filenames must be in the form of ```chr{}.recode.vcf```,
  where ```{}``` needs to be replaced by the chromosome number.
  The input files must be supplied under ```ForwardSimulatorHjLib/target/subset/```.
  
  The input VCF files can contain different number of sites. The number of sites contained in one
  input VCF file for one chromosome equals the number sites that will be contained in the VCF file
  for that chromosome generated from the simulation. Note that this step does not generate
  VCF files from the simulation yet. This step extracts necessary information from the founder generation
  to prepare for generating VCF files from the simulation.
  
  The number of individuals contained in each input VCF file must be same as the input to the simulation.
  
  #### 1.Usage
    
  ```
  bash run.sh --parse generationSize numberOfThread
  ```
    
  __Example:__
  
  ```
  bash run.sh --parse 1000 22
  ```
  
  #### 2. File Output
  
  All the sites and bases in the input files will be stored under ```ForwardSimulatorHjLib/target/ukb/``` in binary.
  They are used internally and are not useful to the users.
    
## 4. Generating VCF Files from Simulation

  #### 1.Usage
    
  ```
  bash run.sh --generate generationSize firstGenerationToStore lastGenerationToStore numberOfThreads
  ```
    
  "firstGenerationToStore" and "lastGenerationToStore" must match the inputs to the simulation. 
    
  __Example:__
  
  ```
  bash run.sh --generate 1000 6 9 22
  ```
  
  This will generate the VCF files for the last 4 generations (6, 7, 8, 9) stored during the simulation,
  using 22 threads.
    
  #### 2. File Output
  
  The output VCF files will be stored under ```ForwardSimulatorHjLib/target/final```
  
## 5. Generating Mapping Files for RaPID

  #### 1.Usage
    
  ```
  bash run.sh --map
  ```
    
  #### 2. File Output
  
  The mapping files (from chromosome positions to genetic positions) will be generated under
  ```ForwardSimulatorHjLib/target/map/```. You can use these files as input to RaPID.
 
## 6. Computing Pairwise Distances

  #### 1.Usage
    
  __Example:__
    
  #### 2. File Output
