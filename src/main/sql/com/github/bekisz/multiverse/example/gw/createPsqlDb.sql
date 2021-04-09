
CREATE TABLE SurvivalByLambda
  (lambda DOUBLE PRECISION PRIMARY KEY, seedSurvivalChance DOUBLE PRECISION, trials BIGINT, err DOUBLE PRECISION);
CREATE TABLE SeedPopulationByTurn
    (lambda DOUBLE PRECISION, turn BIGINT,  seedPopulation DOUBLE PRECISION,
       err DOUBLE PRECISION, trials BIGINT, PRIMARY KEY (lambda,turn));