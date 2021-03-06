/**
  * @author Hang Su <hangsu@gatech.edu>,
  * @author Sungtae An <stan84@gatech.edu>,
  */

package edu.gatech.cse8803.phenotyping

import edu.gatech.cse8803.model.{Diagnostic, LabResult, Medication}
import org.apache.spark.rdd.{PairRDDFunctions, RDD}
import org.apache.spark.{SparkConf, SparkContext}
import java.util.Date

object T2dmPhenotype {

  // criteria codes given
  val T1DM_DX = Set("250.01", "250.03", "250.11", "250.13", "250.21", "250.23",
    "250.31", "250.33", "250.41", "250.43", "250.51", "250.53", "250.61",
    "250.63", "250.71", "250.73", "250.81", "250.83", "250.91", "250.93")

  val T2DM_DX = Set("250.3", "250.32", "250.2", "250.22", "250.9", "250.92",
    "250.8", "250.82", "250.7", "250.72", "250.6", "250.62", "250.5", "250.52",
    "250.4", "250.42", "250.00", "250.02")

  val T1DM_MED = Set("lantus", "insulin glargine", "insulin aspart",
    "insulin detemir", "insulin lente", "insulin nph", "insulin reg",
    "insulin,ultralente")

  val T2DM_MED = Set("chlorpropamide", "diabinese", "diabanase", "diabinase",
    "glipizide", "glucotrol", "glucotrol xl", "glucatrol ", "glyburide",
    "micronase", "glynase", "diabetamide", "diabeta", "glimepiride", "amaryl",
    "repaglinide", "prandin", "nateglinide", "metformin", "rosiglitazone",
    "pioglitazone", "acarbose", "miglitol", "sitagliptin", "exenatide",
    "tolazamide", "acetohexamide", "troglitazone", "tolbutamide", "avandia",
    "actos", "actos", "glipizide")

  val DM_RELATED_DX = Set("790.21", "790.22", "790.2", "790.29", "648.81",
    "648.82", "648.83", "648.84", "648.0", "648.00", "648.01", "648.02",
    "648.03", "648.04", "791.5", "277.7", "V77.1", "256.4", "250.*")
  /**
    * Transform given data set to a RDD of patients and corresponding phenotype
    * @param medication medication RDD
    * @param labResult lab result RDD
    * @param diagnostic diagnostic code RDD
    * @return tuple in the format of (patient-ID, label). label = 1 if the patient is case, label = 2 if control, 3 otherwise
    */
  def transform(medication: RDD[Medication], labResult: RDD[LabResult], diagnostic: RDD[Diagnostic]): RDD[(String, Int)] = {
    /**
      * Remove the place holder and implement your code here.
      * Hard code the medication, lab, icd code etc. for phenotypes like example code below.
      * When testing your code, we expect your function to have no side effect,
      * i.e. do NOT read from file or write file
      *
      * You don't need to follow the example placeholder code below exactly, but do have the same return type.
      *
      * Hint: Consider case sensitivity when doing string comparisons.
      */
    /** Find CASE Patients */
    println("Finding CASE patients...")
    val casePatients = isCase(medication, diagnostic)
    println(casePatients.count())

    /** Find CONTROL Patients */
    println("Finding CONTROL patients...")
    val controlPatients = isControl(labResult, diagnostic).subtract(casePatients)
    println(controlPatients.count())

    /** Find OTHER Patients */
    println("Finding OTHER patients...")
    // union case and control,
    val case_control_id = casePatients
      .union(controlPatients)
      //.map( x => x._1 )

    // map over all arrays, saving patientID, (get all patientIDs)
    val diag_patients = diagnostic.map( x => x.patientID).cache()

    val all_patients = diag_patients.distinct()

    diag_patients.unpersist()

    // difference the sets,
    val others = all_patients
      .subtract(case_control_id)
      .map( x => (x, 3))

    val cases = casePatients.map( x => (x, 1))
    val control = controlPatients.map( x => (x, 2))
    /** Once you find patients for each group, make them as a single RDD[(String, Int)] */
    val phenotypeLabel = cases.union(control).union(others)

    /** Return */
    phenotypeLabel
  }

  def isCase(medication: RDD[Medication], diagnostic: RDD[Diagnostic]): RDD[(String)]= {

    val step1 = diagnostic
      .filter(y => !T1DM_DX.contains(y.code))
      .map( x => x.patientID)

    val step2 = diagnostic
      .filter(y => T2DM_DX.contains(y.code))
      .map( x => x.patientID)

    // All patients that were not T1, but wer T2
    val filteredDiag = step1.intersection(step2).cache()

    val initKeepers = medication
      //.filter(y => T1DM_MED.contains(y.medicine.toLowerCase())) //order t1 meds
      .map( _.patientID)

    val finalKeepers = filteredDiag
        .subtract(initKeepers)
        .map( x => (x, 1))

    val filteredDiagSet = filteredDiag.collect.toSet

    println("NO DIAG")
    println(filteredDiagSet.size)
    println(finalKeepers.count())


    def firstOccurrence(rdd: Iterable[Medication]): Medication = {
      val by_date = rdd.map( x => (x.date, x)).toSeq

      val sorted = scala.util.Sorting.stableSort(by_date,
        (x:(Date, Medication), y:(Date, Medication)) => x._1.before(y._1))
      sorted.head._2
    }

    def rightMeds(rdd: (String, Iterable[Medication])): (String, Int) = {

      var noMedT1 = false
      var noMedT2 = false
      var precedes = false

      val patientID = rdd._1
      val medList = rdd._2

      val medT1 = medList
        .filter(y => T1DM_MED.contains(y.medicine.toLowerCase()))

      noMedT1 = medT1.size == 0

      if ( !noMedT1 ) {
        val medT2 = medList
          .filter(y => T2DM_MED.contains(y.medicine.toLowerCase()))

        noMedT2 = medT2.size == 0

        if ( !noMedT2 ){

          val first_t1 = firstOccurrence(medT1)
          val first_t2 = firstOccurrence(medT2)

          precedes = first_t2.date.before(first_t1.date)

        }
      }

      var output = (patientID, 0)
      if ( noMedT1 || noMedT2 || precedes) {
        output = (patientID, 1)
      }
      output
    }

    val medByID = medication
      .filter( x => filteredDiagSet.contains(x.patientID))
      .map( x => (x.patientID, x))
      .groupByKey()
      .map( rightMeds )
      .filter( x => x._2 == 1 )

    medByID.union(finalKeepers).map( _._1)
  }

  def isControl(labResult: RDD[LabResult], diagnostic: RDD[Diagnostic]): RDD[(String)] = {

    def isAbnormalLab(row: LabResult): Boolean = {
      var out = false
      if (row.testName.toLowerCase() == "hba1c" && row.value >= 6.0) {
        out = true
      }
      else if ( row.testName.toLowerCase() == "hemoglobin a1c" && row.value >= 6.0) {
        out = true
      }
      else if ( row.testName.toLowerCase() == "fasting glucose" && row.value >= 110) {
        out = true
      }
      else if ( row.testName.toLowerCase() == "fasting blood glucose"  && row.value >= 110) {
        out = true
      }
      else if ( row.testName.toLowerCase() == "fasting plasma glucose" && row.value >= 110) {
        out = true
      }
      else if ( row.testName.toLowerCase() == "glucose" && row.value > 110) {
        out = true
      }
      else if ( row.testName.toLowerCase() == "glucose" && row.value > 110) {
        out = true
      }
      else if ( row.testName.toLowerCase() == "glucose, serum" && row.value > 110) {
        out = true
      }

      out
    }


    val glucoseTests = Set( "HbA1c", "Hemoglobin A1c", "Fasting Glucose",
      "Fasting blood glucose", "fasting plasma glucose", "Glucose", "glucose",
      "Glucose, Serum")

    val step1 = labResult
      .filter( x => glucoseTests.contains(x.testName) )
      .map( _.patientID )
      .distinct()

    println("HERE!!!!!!!!!!!!!!!")
    println(step1.count())

    val step3 = labResult
      .filter( x => glucoseTests.contains(x.testName) )
      .filter( x => !isAbnormalLab(x) )
      .map( _.patientID )
      .distinct()


    val step4 = step1.intersection(step3)

    println(step4.count())

    val step2 = diagnostic
      .filter( x => !DM_RELATED_DX.map( y => x.code.matches(y))
                                  .reduce( (acc, z) => acc || z))
      .map( _.patientID)
      .distinct()

    val output = step4.intersection(step2).distinct()

    output
  }
}
