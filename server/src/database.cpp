#include "../include/database.hpp"
#include<iostream>

Database::Database(const string &dbName) {
  if (sqlite3_open(dbName.c_str(), &db) != SQLITE_OK) {
    cerr << "Cannot open database: " << sqlite3_errmsg(db) << endl;
    db = nullptr;
  }
}
Database::~Database() {
  if (db) {
    sqlite3_close(db);
  }
}

bool Database::execute(const string &sql) {
  char *errMsg = nullptr;
  if (sqlite3_exec(db, sql.c_str(), nullptr, nullptr, &errMsg) != SQLITE_OK) {
    cerr << "SQL error: " << errMsg << endl;
    sqlite3_free(errMsg);
    return false;
  }
  return true;
}

vector<vector<string>> Database::query(const string &sql) {
  vector<vector<string>> result;
  char *errMsg = nullptr;
  sqlite3_stmt *stmt;

  if (sqlite3_prepare_v2(db, sql.c_str(), -1, &stmt, nullptr) != SQLITE_OK) {
    cerr << "Failed to prepare statement: " << sqlite3_errmsg(db) << endl;
    return result;
  }

  while (sqlite3_step(stmt) == SQLITE_ROW) {
    vector<string> row;
    for (int i = 0; i < sqlite3_column_count(stmt); ++i) {
      const char *text = (const char *)sqlite3_column_text(stmt, i);
      row.push_back(text ? text : "");
    }
    result.push_back(row);
  }

  sqlite3_finalize(stmt);
  return result;
}
