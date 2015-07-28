package ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.persistence

import ch.uzh.ifi.pdeboer.pplib.util.LazyLogger
import scalikejdbc._

/**
 * Created by mattia on 07.07.15.
 */
object DBInitializer extends LazyLogger {

  def run() {
    DB readOnly { implicit s =>
      //user TABLE
      try {
        sql"select 1 from user limit 1".map(_.long(1)).single.apply()
        logger.debug("Table user already initialized")
      }
      catch {
        case e: java.sql.SQLException =>
          DB autoCommit { implicit s =>
            sql"CREATE TABLE user (id BIGINT NOT NULL AUTO_INCREMENT,turker_id VARCHAR(255) NOT NULL, first_seen_date_time datetime NOT NULL, PRIMARY KEY(id));".execute().apply()
            logger.debug("Table user created")
          }
      }

      //batch TABLE
      try {
        sql"select 1 from batch limit 1".map(_.long(1)).single.apply()
        logger.debug("Table batch already initialized")
      }
      catch {
        case e: java.sql.SQLException =>
          DB autoCommit { implicit s =>
            sql"CREATE TABLE batch (id BIGINT NOT NULL AUTO_INCREMENT,allowed_answers_per_turker INT NOT NULL, uuid VARCHAR(255) NOT NULL, PRIMARY KEY(id));".execute().apply()
            logger.debug("Table batch created")
          }
      }

      //Question TABLE
      try {
        sql"select 1 from question limit 1".map(_.long(1)).single.apply()
        logger.debug("Table question already initialized")
      }
      catch {
        case e: java.sql.SQLException =>
          DB autoCommit { implicit s =>
            sql"CREATE TABLE question (id BIGINT NOT NULL AUTO_INCREMENT, batch_id BIGINT NOT NULL, html LONGTEXT NOT NULL, output_code BIGINT NOT NULL, create_time DATETIME NOT NULL, uuid VARCHAR(255) NOT NULL, PRIMARY KEY(id), FOREIGN KEY(batch_id) REFERENCES batch(id));".execute().apply()
            logger.debug("Table question created")
          }
      }

      //assets TABLE
      try {
        sql"select 1 from assets limit 1".map(_.long(1)).single.apply()
        logger.debug("Table assets already initialized")
      }
      catch {
        case e: java.sql.SQLException =>
          DB autoCommit { implicit s =>
            sql"CREATE TABLE assets (id BIGINT NOT NULL AUTO_INCREMENT, byte_array LONGBLOB NOT NULL, content_type VARCHAR(255) NOT NULL, question_id BIGINT NOT NULL, filename VARCHAR(300) NOT NULL, PRIMARY KEY(id), FOREIGN KEY(question_id) REFERENCES question(id));".execute().apply()
            logger.debug("Table assets created")
          }
      }

      //answer TABLE
      try {
        sql"select 1 from answer limit 1".map(_.long(1)).single.apply()
        logger.debug("Table answer already initialized")
      }
      catch {
        case e: java.sql.SQLException =>
          DB autoCommit { implicit s =>
            sql"CREATE TABLE answer (id BIGINT NOT NULL AUTO_INCREMENT, question_id BIGINT NOT NULL, user_id BIGINT NOT NULL, time datetime NOT NULL, answer_json varchar(1000) NOT NULL, PRIMARY KEY(id), FOREIGN KEY(question_id) REFERENCES question(id), FOREIGN KEY(user_id) REFERENCES user(id));".execute().apply()
            logger.debug("Table answer created")
          }
      }

    }
  }
}
