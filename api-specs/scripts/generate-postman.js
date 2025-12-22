#!/usr/bin/env node

/**
 * Generate Postman collection from OpenAPI specification
 *
 * This script converts the bundled OpenAPI YAML to a Postman collection,
 * ensuring that the collection is always in sync with the API specification.
 */

const fs = require('fs');
const path = require('path');
const Converter = require('openapi-to-postmanv2');

const OPENAPI_FILE = path.join(__dirname, '../dist/tateca-api.yaml');
const OUTPUT_FILE = path.join(__dirname, '../postman/collections/tateca-api-generated.postman_collection.json');

// Check if bundled OpenAPI file exists
if (!fs.existsSync(OPENAPI_FILE)) {
  console.error('❌ Error: Bundled OpenAPI file not found.');
  console.error('   Please run "npm run bundle" first to generate the bundled spec.');
  process.exit(1);
}

// Read OpenAPI specification
const openapiSpec = fs.readFileSync(OPENAPI_FILE, 'utf8');

// Convert options
const options = {
  folderStrategy: 'Tags',
  requestParametersResolution: 'Example',
  exampleParametersResolution: 'Example',
  includeAuthInfoInExample: true,
  schemaFaker: true,
  requestNameSource: 'Fallback',
  indentCharacter: '  '
};

// Convert OpenAPI to Postman collection
Converter.convert(
  { type: 'string', data: openapiSpec },
  options,
  (error, conversionResult) => {
    if (error) {
      console.error('❌ Conversion failed:', error);
      process.exit(1);
    }

    if (!conversionResult.result) {
      console.error('❌ Conversion failed:');
      conversionResult.reason && console.error('   Reason:', conversionResult.reason);
      process.exit(1);
    }

    // Get the collection
    const collection = conversionResult.output[0].data;

    // Add collection metadata
    collection.info.name = 'Tateca API (Auto-generated)';
    collection.info.description = 'Automatically generated from OpenAPI specification. Do not edit manually.';

    // Write to file
    fs.writeFileSync(
      OUTPUT_FILE,
      JSON.stringify(collection, null, 2),
      'utf8'
    );

    console.log('✅ Postman collection generated successfully!');
    console.log(`   Output: ${OUTPUT_FILE}`);

    // Show warnings if any
    if (conversionResult.output[0].type === 'collection' && conversionResult.output[0].warnings) {
      const warnings = conversionResult.output[0].warnings;
      if (warnings.length > 0) {
        console.log('\n⚠️  Warnings:');
        warnings.forEach(warning => {
          console.log(`   - ${warning.message}`);
        });
      }
    }
  }
);
