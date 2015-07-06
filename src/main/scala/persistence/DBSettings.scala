package persistence

/**
 * Created by Mattia on 19.01.2015.
 */

import scalikejdbc._
import scalikejdbc.config._

trait DBSettings {
  DBSettings.initialize()
}

object DBSettings {

  private var isInitialized = false

  def initialize(): Unit = this.synchronized {
    if (isInitialized) return
    DBs.setupAll()

    GlobalSettings.loggingSQLErrors = true
    //GlobalSettings.sqlFormatter = SQLFormatterSettings("devteam.misc.HibernateSQLFormatter")
    DBInitializer.run()
    isInitialized = true
  }

}

object DBInitializer {
  def run() {
    DB readOnly { implicit s =>
      //user TABLE
      try {
        sql"select 1 from user limit 1".map(_.long(1)).single.apply()
      }
      catch {
        case e: java.sql.SQLException =>
          DB autoCommit { implicit s =>
            sql"CREATE TABLE user (id BIGINT NOT NULL AUTO_INCREMENT,turker_id VARCHAR(255) NOT NULL, first_seen_date_time datetime NOT NULL, PRIMARY KEY(id));".execute().apply()
          }
      }

      //batch TABLE
      try {
        sql"select 1 from batch limit 1".map(_.long(1)).single.apply()
      }
      catch {
        case e: java.sql.SQLException =>
          DB autoCommit { implicit s =>
            sql"CREATE TABLE batch (id BIGINT NOT NULL AUTO_INCREMENT,allowed_answers_per_turker INT NOT NULL, PRIMARY KEY(id));".execute().apply()
          }
      }

      //Question TABLE
      try {
        sql"select 1 from question limit 1".map(_.long(1)).single.apply()
      }
      catch {
        case e: java.sql.SQLException =>
          DB autoCommit { implicit s =>
            sql"CREATE TABLE question (id BIGINT NOT NULL AUTO_INCREMENT,html VARCHAR(10000) NOT NULL,question_type VARCHAR(255) NOT NULL,output_code BIGINT NOT NULL,batch_id BIGINT NOT NULL,create_time datetime NOT NULL,uuid VARCHAR(255) NOT NULL, PRIMARY KEY(id), FOREIGN KEY(batch_id) REFERENCES batch(id));".execute().apply()
          }
      }

      //answer TABLE
      try {
        sql"select 1 from answer limit 1".map(_.long(1)).single.apply()
      }
      catch {
        case e: java.sql.SQLException =>
          DB autoCommit { implicit s =>
            sql"CREATE TABLE answer (id BIGINT NOT NULL AUTO_INCREMENT, question_id BIGINT NOT NULL, user_id BIGINT NOT NULL, time datetime NOT NULL, answer_json varchar(1000) NOT NULL, PRIMARY KEY(id), FOREIGN KEY(question_id) REFERENCES question(id), FOREIGN KEY(user_id) REFERENCES user(id));".execute().apply()
          }
      }
    }
  }
}