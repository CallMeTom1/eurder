import pg from 'pg';

const database = process.env.E2E_DB_NAME ?? 'eurder_e2e';

const client = new pg.Client({
  host: process.env.E2E_DB_HOST ?? 'localhost',
  port: Number(process.env.E2E_DB_PORT ?? 5432),
  user: process.env.E2E_DB_USERNAME ?? 'postgres',
  password: process.env.E2E_DB_PASSWORD ?? 'postgres',
  database: 'postgres',
});

await client.connect();
await client.query(`drop database if exists ${database} with (force)`);
await client.query(`create database ${database}`);
await client.end();
console.log(`Database ${database} recreated, Flyway migrates it when the API starts.`);
