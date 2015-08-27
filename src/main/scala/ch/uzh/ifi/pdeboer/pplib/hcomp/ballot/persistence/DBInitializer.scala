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

      //permutations TABLE
      try {
        sql"select 1 from permutations limit 1".map(_.long(1)).single.apply()
        logger.debug("Table permutation already initialized")
      }
      catch {
        case e: java.sql.SQLException =>
          DB autoCommit { implicit s =>
            sql"CREATE TABLE permutations (id BIGINT NOT NULL AUTO_INCREMENT, group_name VARCHAR(255) NOT NULL, method_index VARCHAR(255) NOT NULL, snippet_filename VARCHAR(255) NOT NULL, pdf_path VARCHAR(255) NOT NULL, method_on_top bool not null, state BIGINT NOT NULL DEFAULT 0, excluded_step INT DEFAULT 0, relative_height_top DOUBLE(5,2) NOT NULL, relative_height_bottom DOUBLE(5,2) NOT NULL, PRIMARY KEY(id));".execute().apply()
            logger.debug("Table permutations created")
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
            sql"CREATE TABLE question (id BIGINT NOT NULL AUTO_INCREMENT, batch_id BIGINT NOT NULL, html LONGTEXT NOT NULL, create_time DATETIME NOT NULL, uuid VARCHAR(255) NOT NULL, hints BIGINT NOT NULL, PRIMARY KEY(id), FOREIGN KEY(batch_id) REFERENCES batch(id), FOREIGN KEY(hints) REFERENCES permutations(id));".execute().apply()
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
            sql"CREATE TABLE answer (id BIGINT NOT NULL AUTO_INCREMENT, question_id BIGINT NOT NULL, user_id BIGINT NOT NULL, time datetime NOT NULL, answer_json LONGTEXT NOT NULL, expected_output_code BIGINT NOT NULL, accepted bool not null default 0, PRIMARY KEY(id), FOREIGN KEY(question_id) REFERENCES question(id), FOREIGN KEY(user_id) REFERENCES user(id));".execute().apply()
            logger.debug("Table answer created")
          }
      }
    }
  }
}
