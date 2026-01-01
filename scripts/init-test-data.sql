-- ============================================================================
-- Initial test data for local development
-- Usage:
--   1. Via test-api.sh (recommended):
--      ./scripts/test-api.sh
--
--   2. Manual execution:
--      mysql -h localhost -P 3306 -u a -pa db < scripts/init-test-data.sql
--
-- Note: This script assumes V1__Initial_schema.sql has been applied by Flyway
-- ============================================================================

USE db;

-- ============================================================================
-- Clean existing test data (for re-initialization)
-- ============================================================================
DELETE FROM transaction_obligations WHERE 1=1;
DELETE FROM transaction_history WHERE 1=1;
DELETE FROM user_groups WHERE 1=1;
DELETE FROM `groups` WHERE 1=1;
DELETE FROM users WHERE 1=1;
DELETE FROM exchange_rates WHERE 1=1;
DELETE FROM currency_names WHERE 1=1;
DELETE FROM auth_users WHERE 1=1;

-- ============================================================================
-- Auth Users (Firebase authenticated users)
-- ============================================================================
INSERT INTO auth_users (uid, name, email, created_at, updated_at, total_login_count, app_review_status) VALUES
('test-user-001', 'Test User 1', 'test1@example.com', NOW(), NOW(), 5, 'PENDING'),
('test-user-002', 'Test User 2', 'test2@example.com', NOW(), NOW(), 3, 'PENDING'),
('test-user-003', 'Test User 3', 'test3@example.com', NOW(), NOW(), 0, 'PENDING');

-- ============================================================================
-- Users (Group members)
-- ============================================================================
INSERT INTO users (uuid, name, auth_user_uid, created_at, updated_at) VALUES
(UUID_TO_BIN('550e8400-e29b-41d4-a716-446655440001'), 'Alice', 'test-user-001', NOW(), NOW()),
(UUID_TO_BIN('550e8400-e29b-41d4-a716-446655440002'), 'Bob', 'test-user-002', NOW(), NOW()),
(UUID_TO_BIN('550e8400-e29b-41d4-a716-446655440003'), 'Charlie', NULL, NOW(), NOW()),
(UUID_TO_BIN('550e8400-e29b-41d4-a716-446655440004'), 'Dave', NULL, NOW(), NOW());

-- ============================================================================
-- Groups (Expense sharing groups)
-- ============================================================================
INSERT INTO `groups` (uuid, name, join_token, token_expires, created_at, updated_at) VALUES
(UUID_TO_BIN('650e8400-e29b-41d4-a716-446655440001'), 'Team Outing 2024', UUID_TO_BIN('750e8400-e29b-41d4-a716-446655440001'), DATE_ADD(NOW(), INTERVAL 1 DAY), NOW(), NOW()),
(UUID_TO_BIN('650e8400-e29b-41d4-a716-446655440002'), 'Weekend Trip', UUID_TO_BIN('750e8400-e29b-41d4-a716-446655440002'), DATE_ADD(NOW(), INTERVAL 7 DAY), NOW(), NOW());

-- ============================================================================
-- User Groups (Group membership)
-- ============================================================================
INSERT INTO user_groups (user_uuid, group_uuid) VALUES
(UUID_TO_BIN('550e8400-e29b-41d4-a716-446655440001'), UUID_TO_BIN('650e8400-e29b-41d4-a716-446655440001')),
(UUID_TO_BIN('550e8400-e29b-41d4-a716-446655440002'), UUID_TO_BIN('650e8400-e29b-41d4-a716-446655440001')),
(UUID_TO_BIN('550e8400-e29b-41d4-a716-446655440001'), UUID_TO_BIN('650e8400-e29b-41d4-a716-446655440002')),
(UUID_TO_BIN('550e8400-e29b-41d4-a716-446655440004'), UUID_TO_BIN('650e8400-e29b-41d4-a716-446655440002'));

-- ============================================================================
-- Currency Names (162 currencies)
-- Note: Exchange rates will be populated via /internal/exchange-rates API
-- ============================================================================
INSERT INTO currency_names (currency_code, jp_currency_name, eng_currency_name, jp_country_name, eng_country_name, is_active, currency_symbol, symbol_position) VALUES
('AED', 'UAEディルハム', 'UAE Dirham', 'アラブ首長国連邦', 'United Arab Emirates', 1, 'د.إ', 'PREFIX'),
('AFN', 'アフガニ', 'Afghan Afghani', 'アフガニスタン', 'Afghanistan', 1, '؋', 'SUFFIX'),
('ALL', 'アルバニア・レク', 'Albanian Lek', 'アルバニア', 'Albania', 1, 'L', 'SUFFIX'),
('AMD', 'アルメニア・ドラム', 'Armenian Dram', 'アルメニア', 'Armenia', 1, '֏', 'SUFFIX'),
('ANG', 'オランダ領アンティル・ギルダー', 'Netherlands Antillian Guilder', 'オランダ領アンティル', 'Netherlands Antilles', 1, 'ƒ', 'SUFFIX'),
('AOA', 'アンゴラ・クワンザ', 'Angolan Kwanza', 'アンゴラ', 'Angola', 1, 'Kz', 'SUFFIX'),
('ARS', 'アルゼンチン・ペソ', 'Argentine Peso', 'アルゼンチン', 'Argentina', 1, '$', 'PREFIX'),
('AUD', 'オーストラリア・ドル', 'Australian Dollar', 'オーストラリア', 'Australia', 1, '$', 'PREFIX'),
('AWG', 'アルバ・フローリン', 'Aruban Florin', 'アルバ', 'Aruba', 1, 'ƒ', 'SUFFIX'),
('AZN', 'アゼルバイジャン・マナット', 'Azerbaijani Manat', 'アゼルバイジャン', 'Azerbaijan', 1, '₼', 'SUFFIX'),
('BAM', 'ボスニア・ヘルツェゴビナ・マルク', 'Bosnia and Herzegovina Mark', 'ボスニア・ヘルツェゴビナ', 'Bosnia and Herzegovina', 1, 'KM', 'SUFFIX'),
('BBD', 'バルバドス・ドル', 'Barbados Dollar', 'バルバドス', 'Barbados', 1, '$', 'PREFIX'),
('BDT', 'バングラデシュ・タカ', 'Bangladeshi Taka', 'バングラデシュ', 'Bangladesh', 1, '৳', 'SUFFIX'),
('BGN', 'ブルガリア・レフ', 'Bulgarian Lev', 'ブルガリア', 'Bulgaria', 1, 'лв', 'SUFFIX'),
('BHD', 'バーレーン・ディナール', 'Bahraini Dinar', 'バーレーン', 'Bahrain', 1, 'د.ب', 'PREFIX'),
('BIF', 'ブルンジ・フラン', 'Burundian Franc', 'ブルンジ', 'Burundi', 1, 'FBu', 'SUFFIX'),
('BMD', 'バミューダ・ドル', 'Bermudian Dollar', 'バミューダ', 'Bermuda', 1, '$', 'PREFIX'),
('BND', 'ブルネイ・ドル', 'Brunei Dollar', 'ブルネイ', 'Brunei', 1, '$', 'PREFIX'),
('BOB', 'ボリビア・ボリビアーノ', 'Bolivian Boliviano', 'ボリビア', 'Bolivia', 1, 'Bs', 'SUFFIX'),
('BRL', 'ブラジル・レアル', 'Brazilian Real', 'ブラジル', 'Brazil', 1, 'R$', 'PREFIX'),
('BSD', 'バハマ・ドル', 'Bahamian Dollar', 'バハマ', 'Bahamas', 1, '$', 'PREFIX'),
('BTN', 'ブータン・ヌルトルム', 'Bhutanese Ngultrum', 'ブータン', 'Bhutan', 1, 'Nu.', 'SUFFIX'),
('BWP', 'ボツワナ・プラ', 'Botswana Pula', 'ボツワナ', 'Botswana', 1, 'P', 'SUFFIX'),
('BYN', 'ベラルーシ・ルーブル', 'Belarusian Ruble', 'ベラルーシ', 'Belarus', 1, '₽', 'SUFFIX'),
('BZD', 'ベリーズ・ドル', 'Belize Dollar', 'ベリーズ', 'Belize', 1, '$', 'PREFIX'),
('CAD', 'カナダ・ドル', 'Canadian Dollar', 'カナダ', 'Canada', 1, '$', 'PREFIX'),
('CDF', 'コンゴ・フラン', 'Congolese Franc', 'コンゴ', 'Democratic Republic of the Congo', 1, 'FC', 'SUFFIX'),
('CHF', 'スイス・フラン', 'Swiss Franc', 'スイス', 'Switzerland', 1, 'CHF', 'SUFFIX'),
('CLP', 'チリ・ペソ', 'Chilean Peso', 'チリ', 'Chile', 1, '$', 'PREFIX'),
('CNY', '中国元', 'Chinese Renminbi', '中国', 'China', 1, '¥', 'PREFIX'),
('COP', 'コロンビア・ペソ', 'Colombian Peso', 'コロンビア', 'Colombia', 1, '$', 'PREFIX'),
('CRC', 'コスタリカ・コロン', 'Costa Rican Colon', 'コスタリカ', 'Costa Rica', 1, '₡', 'SUFFIX'),
('CUP', 'キューバ・ペソ', 'Cuban Peso', 'キューバ', 'Cuba', 1, '$', 'PREFIX'),
('CVE', 'カーボベルデ・エスクード', 'Cape Verdean Escudo', 'カーボベルデ', 'Cape Verde', 1, 'Esc', 'SUFFIX'),
('CZK', 'チェコ・コルナ', 'Czech Koruna', 'チェコ', 'Czech Republic', 1, 'Kč', 'SUFFIX'),
('DJF', 'ジブチ・フラン', 'Djiboutian Franc', 'ジブチ', 'Djibouti', 1, 'Fdj', 'SUFFIX'),
('DKK', 'デンマーク・クローネ', 'Danish Krone', 'デンマーク', 'Denmark', 1, 'kr', 'SUFFIX'),
('DOP', 'ドミニカ・ペソ', 'Dominican Peso', 'ドミニカ共和国', 'Dominican Republic', 1, 'RD$', 'PREFIX'),
('DZD', 'アルジェリア・ディナール', 'Algerian Dinar', 'アルジェリア', 'Algeria', 1, 'د.ج', 'PREFIX'),
('EGP', 'エジプト・ポンド', 'Egyptian Pound', 'エジプト', 'Egypt', 1, '£', 'PREFIX'),
('ERN', 'エリトリア・ナクファ', 'Eritrean Nakfa', 'エリトリア', 'Eritrea', 1, 'Nfk', 'SUFFIX'),
('ETB', 'エチオピア・ビル', 'Ethiopian Birr', 'エチオピア', 'Ethiopia', 1, 'Br', 'SUFFIX'),
('EUR', 'ユーロ', 'Euro', 'ヨーロッパ連合', 'European Union', 1, '€', 'PREFIX'),
('FJD', 'フィジー・ドル', 'Fiji Dollar', 'フィジー', 'Fiji', 1, '$', 'PREFIX'),
('FKP', 'フォークランド諸島・ポンド', 'Falkland Islands Pound', 'フォークランド諸島', 'Falkland Islands', 1, '£', 'PREFIX'),
('FOK', 'フェロー諸島・クローナ', 'Faroese Króna', 'フェロー諸島', 'Faroe Islands', 1, 'kr', 'SUFFIX'),
('GBP', 'ポンド', 'Pound Sterling', 'イギリス', 'United Kingdom', 1, '£', 'PREFIX'),
('GEL', 'ジョージア・ラリ', 'Georgian Lari', 'ジョージア', 'Georgia', 1, '₾', 'SUFFIX'),
('GGP', 'ガーンジー・ポンド', 'Guernsey Pound', 'ガーンジー', 'Guernsey', 1, '£', 'PREFIX'),
('GHS', 'ガーナ・セディ', 'Ghanaian Cedi', 'ガーナ', 'Ghana', 1, '₵', 'SUFFIX'),
('GIP', 'ジブラルタル・ポンド', 'Gibraltar Pound', 'ジブラルタル', 'Gibraltar', 1, '£', 'PREFIX'),
('GMD', 'ガンビア・ダラシ', 'Gambian Dalasi', 'ガンビア', 'The Gambia', 1, 'D', 'SUFFIX'),
('GNF', 'ギニア・フラン', 'Guinean Franc', 'ギニア', 'Guinea', 1, 'FG', 'SUFFIX'),
('GTQ', 'グアテマラ・ケツァール', 'Guatemalan Quetzal', 'グアテマラ', 'Guatemala', 1, 'Q', 'SUFFIX'),
('GYD', 'ガイアナ・ドル', 'Guyanese Dollar', 'ガイアナ', 'Guyana', 1, '$', 'PREFIX'),
('HKD', '香港・ドル', 'Hong Kong Dollar', '香港', 'Hong Kong', 1, '$', 'PREFIX'),
('HNL', 'ホンジュラス・レンピラ', 'Honduran Lempira', 'ホンジュラス', 'Honduras', 1, 'L', 'SUFFIX'),
('HRK', 'クロアチア・クーナ', 'Croatian Kuna', 'クロアチア', 'Croatia', 1, 'kn', 'SUFFIX'),
('HTG', 'ハイチ・グルード', 'Haitian Gourde', 'ハイチ', 'Haiti', 1, 'G', 'SUFFIX'),
('HUF', 'ハンガリー・フォリント', 'Hungarian Forint', 'ハンガリー', 'Hungary', 1, 'Ft', 'SUFFIX'),
('IDR', 'インドネシア・ルピア', 'Indonesian Rupiah', 'インドネシア', 'Indonesia', 1, 'Rp', 'PREFIX'),
('ILS', 'イスラエル・新シェケル', 'Israeli New Shekel', 'イスラエル', 'Israel', 1, '₪', 'PREFIX'),
('IMP', 'マン島・ポンド', 'Manx Pound', 'マン島', 'Isle of Man', 1, '£', 'PREFIX'),
('INR', 'インド・ルピー', 'Indian Rupee', 'インド', 'India', 1, '₹', 'PREFIX'),
('IQD', 'イラク・ディナール', 'Iraqi Dinar', 'イラク', 'Iraq', 1, 'ع.د', 'PREFIX'),
('IRR', 'イラン・リアル', 'Iranian Rial', 'イラン', 'Iran', 1, '﷼', 'SUFFIX'),
('ISK', 'アイスランド・クローナ', 'Icelandic Króna', 'アイスランド', 'Iceland', 1, 'kr', 'SUFFIX'),
('JEP', 'ジャージー・ポンド', 'Jersey Pound', 'ジャージー', 'Jersey', 1, '£', 'PREFIX'),
('JMD', 'ジャマイカ・ドル', 'Jamaican Dollar', 'ジャマイカ', 'Jamaica', 1, '$', 'PREFIX'),
('JOD', 'ヨルダン・ディナール', 'Jordanian Dinar', 'ヨルダン', 'Jordan', 1, 'د.ا', 'PREFIX'),
('JPY', '日本円', 'Japanese Yen', '日本', 'Japan', 1, '¥', 'PREFIX'),
('KES', 'ケニア・シリング', 'Kenyan Shilling', 'ケニア', 'Kenya', 1, 'Sh', 'SUFFIX'),
('KGS', 'キルギス・ソム', 'Kyrgyzstani Som', 'キルギス', 'Kyrgyzstan', 1, 'лв', 'SUFFIX'),
('KHR', 'カンボジア・リエル', 'Cambodian Riel', 'カンボジア', 'Cambodia', 1, '៛', 'SUFFIX'),
('KID', 'キリバス・ドル', 'Kiribati Dollar', 'キリバス', 'Kiribati', 1, '$', 'PREFIX'),
('KMF', 'コモロ・フラン', 'Comorian Franc', 'コモロ', 'Comoros', 1, 'CF', 'SUFFIX'),
('KRW', '韓国・ウォン', 'South Korean Won', '韓国', 'South Korea', 1, '₩', 'PREFIX'),
('KWD', 'クウェート・ディナール', 'Kuwaiti Dinar', 'クウェート', 'Kuwait', 1, 'د.ك', 'PREFIX'),
('KYD', 'ケイマン諸島・ドル', 'Cayman Islands Dollar', 'ケイマン諸島', 'Cayman Islands', 1, '$', 'PREFIX'),
('KZT', 'カザフスタン・テンゲ', 'Kazakhstani Tenge', 'カザフスタン', 'Kazakhstan', 1, '₸', 'SUFFIX'),
('LAK', 'ラオス・キップ', 'Lao Kip', 'ラオス', 'Laos', 1, '₭', 'SUFFIX'),
('LBP', 'レバノン・ポンド', 'Lebanese Pound', 'レバノン', 'Lebanon', 1, 'ل.ل', 'PREFIX'),
('LKR', 'スリランカ・ルピー', 'Sri Lanka Rupee', 'スリランカ', 'Sri Lanka', 1, 'Rs', 'SUFFIX'),
('LRD', 'リベリア・ドル', 'Liberian Dollar', 'リベリア', 'Liberia', 1, '$', 'PREFIX'),
('LSL', 'レソト・ロティ', 'Lesotho Loti', 'レソト', 'Lesotho', 1, 'M', 'SUFFIX'),
('LYD', 'リビア・ディナール', 'Libyan Dinar', 'リビア', 'Libya', 1, 'د.ل', 'PREFIX'),
('MAD', 'モロッコ・ディルハム', 'Moroccan Dirham', 'モロッコ', 'Morocco', 1, 'د.م.', 'PREFIX'),
('MDL', 'モルドバ・レウ', 'Moldovan Leu', 'モルドバ', 'Moldova', 1, 'L', 'SUFFIX'),
('MGA', 'マダガスカル・アリアリ', 'Malagasy Ariary', 'マダガスカル', 'Madagascar', 1, 'Ar', 'SUFFIX'),
('MKD', '北マケドニア・デナル', 'Macedonian Denar', '北マケドニア', 'North Macedonia', 1, 'ден', 'SUFFIX'),
('MMK', 'ミャンマー・チャット', 'Burmese Kyat', 'ミャンマー', 'Myanmar', 1, 'K', 'SUFFIX'),
('MNT', 'モンゴル・トグログ', 'Mongolian Tögrög', 'モンゴル', 'Mongolia', 1, '₮', 'SUFFIX'),
('MOP', 'マカオ・パタカ', 'Macanese Pataca', 'マカオ', 'Macau', 1, 'MOP', 'SUFFIX'),
('MRU', 'モーリタニア・ウギーヤ', 'Mauritanian Ouguiya', 'モーリタニア', 'Mauritania', 1, 'UM', 'SUFFIX'),
('MUR', 'モーリシャス・ルピー', 'Mauritian Rupee', 'モーリシャス', 'Mauritius', 1, 'Rs', 'SUFFIX'),
('MVR', 'モルディブ・ルフィア', 'Maldivian Rufiyaa', 'モルディブ', 'Maldives', 1, 'Rf', 'SUFFIX'),
('MWK', 'マラウイ・クワチャ', 'Malawian Kwacha', 'マラウイ', 'Malawi', 1, 'MK', 'SUFFIX'),
('MXN', 'メキシコ・ペソ', 'Mexican Peso', 'メキシコ', 'Mexico', 1, '$', 'PREFIX'),
('MYR', 'マレーシア・リンギット', 'Malaysian Ringgit', 'マレーシア', 'Malaysia', 1, 'RM', 'PREFIX'),
('MZN', 'モザンビーク・メティカル', 'Mozambican Metical', 'モザンビーク', 'Mozambique', 1, 'MT', 'SUFFIX'),
('NAD', 'ナミビア・ドル', 'Namibian Dollar', 'ナミビア', 'Namibia', 1, '$', 'PREFIX'),
('NGN', 'ナイジェリア・ナイラ', 'Nigerian Naira', 'ナイジェリア', 'Nigeria', 1, '₦', 'PREFIX'),
('NIO', 'ニカラグア・コルドバ', 'Nicaraguan Córdoba', 'ニカラグア', 'Nicaragua', 1, 'C$', 'PREFIX'),
('NOK', 'ノルウェー・クローネ', 'Norwegian Krone', 'ノルウェー', 'Norway', 1, 'kr', 'SUFFIX'),
('NPR', 'ネパール・ルピー', 'Nepalese Rupee', 'ネパール', 'Nepal', 1, 'Rs', 'SUFFIX'),
('NZD', 'ニュージーランド・ドル', 'New Zealand Dollar', 'ニュージーランド', 'New Zealand', 1, '$', 'PREFIX'),
('OMR', 'オマーン・リアル', 'Omani Rial', 'オマーン', 'Oman', 1, 'ر.ع.', 'PREFIX'),
('PAB', 'パナマ・バルボア', 'Panamanian Balboa', 'パナマ', 'Panama', 1, 'B/.', 'PREFIX'),
('PEN', 'ペルー・ソル', 'Peruvian Sol', 'ペルー', 'Peru', 1, 'S/', 'PREFIX'),
('PGK', 'パプアニューギニア・キナ', 'Papua New Guinean Kina', 'パプアニューギニア', 'Papua New Guinea', 1, 'K', 'SUFFIX'),
('PHP', 'フィリピン・ペソ', 'Philippine Peso', 'フィリピン', 'Philippines', 1, '₱', 'PREFIX'),
('PKR', 'パキスタン・ルピー', 'Pakistani Rupee', 'パキスタン', 'Pakistan', 1, 'Rs', 'SUFFIX'),
('PLN', 'ポーランド・ズロチ', 'Polish Złoty', 'ポーランド', 'Poland', 1, 'zł', 'SUFFIX'),
('PYG', 'パラグアイ・グアラニー', 'Paraguayan Guaraní', 'パラグアイ', 'Paraguay', 1, '₲', 'SUFFIX'),
('QAR', 'カタール・リヤル', 'Qatari Riyal', 'カタール', 'Qatar', 1, 'ر.ق', 'PREFIX'),
('RON', 'ルーマニア・レウ', 'Romanian Leu', 'ルーマニア', 'Romania', 1, 'lei', 'SUFFIX'),
('RSD', 'セルビア・ディナール', 'Serbian Dinar', 'セルビア', 'Serbia', 1, 'Дин.', 'SUFFIX'),
('RUB', 'ロシア・ルーブル', 'Russian Ruble', 'ロシア', 'Russia', 1, '₽', 'SUFFIX'),
('RWF', 'ルワンダ・フラン', 'Rwandan Franc', 'ルワンダ', 'Rwanda', 1, 'Fr', 'SUFFIX'),
('SAR', 'サウジアラビア・リヤル', 'Saudi Riyal', 'サウジアラビア', 'Saudi Arabia', 1, 'ر.س', 'PREFIX'),
('SBD', 'ソロモン諸島・ドル', 'Solomon Islands Dollar', 'ソロモン諸島', 'Solomon Islands', 1, '$', 'PREFIX'),
('SCR', 'セイシェル・ルピー', 'Seychellois Rupee', 'セイシェル', 'Seychelles', 1, 'Rs', 'SUFFIX'),
('SDG', 'スーダン・ポンド', 'Sudanese Pound', 'スーダン', 'Sudan', 1, '£', 'SUFFIX'),
('SEK', 'スウェーデン・クローナ', 'Swedish Krona', 'スウェーデン', 'Sweden', 1, 'kr', 'SUFFIX'),
('SGD', 'シンガポール・ドル', 'Singapore Dollar', 'シンガポール', 'Singapore', 1, '$', 'PREFIX'),
('SHP', 'セントヘレナ・ポンド', 'Saint Helena Pound', 'セントヘレナ', 'Saint Helena', 1, '£', 'SUFFIX'),
('SLE', 'シエラレオネ・レオネ', 'Sierra Leonean Leone', 'シエラレオネ', 'Sierra Leone', 1, 'Le', 'SUFFIX'),
('SLL', 'シエラレオネ・レオネ', 'Sierra Leonean Leone', 'シエラレオネ', 'Sierra Leone', 0, 'Le', 'SUFFIX'),
('SOS', 'ソマリア・シリング', 'Somali Shilling', 'ソマリア', 'Somalia', 1, 'Sh', 'SUFFIX'),
('SRD', 'スリナム・ドル', 'Surinamese Dollar', 'スリナム', 'Suriname', 1, '$', 'PREFIX'),
('SSP', '南スーダン・ポンド', 'South Sudanese Pound', '南スーダン', 'South Sudan', 1, '£', 'SUFFIX'),
('STN', 'サントメ・プリンシペ・ドブラ', 'São Tomé and Príncipe Dobra', 'サントメ・プリンシペ', 'São Tomé and Príncipe', 1, 'Db', 'SUFFIX'),
('SYP', 'シリア・ポンド', 'Syrian Pound', 'シリア', 'Syria', 1, '£', 'SUFFIX'),
('SZL', 'エスワティニ・リランゲニ', 'Eswatini Lilangeni', 'エスワティニ', 'Eswatini', 1, 'E', 'SUFFIX'),
('THB', 'タイ・バーツ', 'Thai Baht', 'タイ', 'Thailand', 1, '฿', 'SUFFIX'),
('TJS', 'タジキスタン・ソモニ', 'Tajikistani Somoni', 'タジキスタン', 'Tajikistan', 1, 'SM', 'SUFFIX'),
('TMT', 'トルクメニスタン・マナット', 'Turkmenistan Manat', 'トルクメニスタン', 'Turkmenistan', 1, 'm', 'SUFFIX'),
('TND', 'チュニジア・ディナール', 'Tunisian Dinar', 'チュニジア', 'Tunisia', 1, 'د.ت', 'PREFIX'),
('TOP', 'トンガ・パアンガ', 'Tongan Paʻanga', 'トンガ', 'Tonga', 1, 'T$', 'PREFIX'),
('TRY', 'トルコ・リラ', 'Turkish Lira', 'トルコ', 'Turkey', 1, '₺', 'PREFIX'),
('TTD', 'トリニダード・トバゴ・ドル', 'Trinidad and Tobago Dollar', 'トリニダード・トバゴ', 'Trinidad and Tobago', 1, '$', 'PREFIX'),
('TVD', 'ツバル・ドル', 'Tuvaluan Dollar', 'ツバル', 'Tuvalu', 1, '$', 'PREFIX'),
('TWD', '新台湾ドル', 'New Taiwan Dollar', '台湾', 'Taiwan', 1, '$', 'PREFIX'),
('TZS', 'タンザニア・シリング', 'Tanzanian Shilling', 'タンザニア', 'Tanzania', 1, 'Sh', 'SUFFIX'),
('UAH', 'ウクライナ・グリフニャ', 'Ukrainian Hryvnia', 'ウクライナ', 'Ukraine', 1, '₴', 'PREFIX'),
('UGX', 'ウガンダ・シリング', 'Ugandan Shilling', 'ウガンダ', 'Uganda', 1, 'Sh', 'SUFFIX'),
('USD', 'アメリカ・ドル', 'United States Dollar', 'アメリカ合衆国', 'United States', 1, '$', 'PREFIX'),
('UYU', 'ウルグアイ・ペソ', 'Uruguayan Peso', 'ウルグアイ', 'Uruguay', 1, '$', 'PREFIX'),
('UZS', 'ウズベキスタン・ソム', 'Uzbekistani So''m', 'ウズベキスタン', 'Uzbekistan', 1, 'сум', 'SUFFIX'),
('VES', 'ベネズエラ・ボリバール・ソベラノ', 'Venezuelan Bolívar Soberano', 'ベネズエラ', 'Venezuela', 1, 'Bs.S', 'SUFFIX'),
('VND', 'ベトナム・ドン', 'Vietnamese Đồng', 'ベトナム', 'Vietnam', 1, '₫', 'SUFFIX'),
('VUV', 'バヌアツ・バツ', 'Vanuatu Vatu', 'バヌアツ', 'Vanuatu', 1, 'Vt', 'SUFFIX'),
('WST', 'サモア・タラ', 'Samoan Tālā', 'サモア', 'Samoa', 1, 'T$', 'PREFIX'),
('XAF', '中央アフリカCFAフラン', 'Central African CFA Franc', '中央アフリカ', 'CEMAC', 1, 'CFA', 'SUFFIX'),
('XCD', '東カリブ・ドル', 'East Caribbean Dollar', '東カリブ諸国', 'Organisation of Eastern Caribbean States', 1, '$', 'PREFIX'),
('XDR', '特別引出権', 'Special Drawing Rights', '国際通貨基金', 'International Monetary Fund', 1, 'XDR', 'SUFFIX'),
('XOF', '西アフリカCFAフラン', 'West African CFA franc', '西アフリカ', 'CFA', 1, 'CFA', 'SUFFIX'),
('XPF', 'CFPフラン', 'CFP Franc', 'フランス海外領土', 'Collectivités d''Outre-Mer', 1, 'XPF', 'SUFFIX'),
('YER', 'イエメン・リアル', 'Yemeni Rial', 'イエメン', 'Yemen', 1, '﷼', 'PREFIX'),
('ZAR', '南アフリカ・ランド', 'South African Rand', '南アフリカ', 'South Africa', 1, 'R', 'SUFFIX'),
('ZMW', 'ザンビア・クワチャ', 'Zambian Kwacha', 'ザンビア', 'Zambia', 1, 'ZK', 'SUFFIX'),
('ZWL', 'ジンバブエ・ドル', 'Zimbabwean Dollar', 'ジンバブエ', 'Zimbabwe', 1, '$', 'PREFIX');

-- ============================================================================
-- Transaction History (Sample transactions)
-- Note: exchange_rate_date uses CURDATE() to dynamically set to current date
-- ============================================================================
INSERT INTO transaction_history (uuid, transaction_type, group_uuid, title, amount, currency_code, exchange_rate_date, transaction_date, payer_id, created_at, updated_at) VALUES
(UUID_TO_BIN('850e8400-e29b-41d4-a716-446655440001'), 'LOAN', UUID_TO_BIN('650e8400-e29b-41d4-a716-446655440001'), 'Dinner at restaurant', 5000, 'JPY', CURDATE(), DATE_SUB(NOW(), INTERVAL 2 DAY), UUID_TO_BIN('550e8400-e29b-41d4-a716-446655440001'), NOW(), NOW()),
(UUID_TO_BIN('850e8400-e29b-41d4-a716-446655440002'), 'LOAN', UUID_TO_BIN('650e8400-e29b-41d4-a716-446655440001'), 'Coffee shop', 2000, 'JPY', CURDATE(), DATE_SUB(NOW(), INTERVAL 1 DAY), UUID_TO_BIN('550e8400-e29b-41d4-a716-446655440002'), NOW(), NOW()),
(UUID_TO_BIN('850e8400-e29b-41d4-a716-446655440003'), 'REPAYMENT', UUID_TO_BIN('650e8400-e29b-41d4-a716-446655440001'), 'Repay Bob', 1500, 'JPY', CURDATE(), DATE_SUB(NOW(), INTERVAL 1 HOUR), UUID_TO_BIN('550e8400-e29b-41d4-a716-446655440001'), NOW(), NOW());

-- ============================================================================
-- Transaction Obligations (Who owes whom)
-- ============================================================================
INSERT INTO transaction_obligations (uuid, transaction_uuid, user_uuid, amount, created_at, updated_at) VALUES
-- Dinner: Bob owes Alice 2500 yen
(UUID_TO_BIN('950e8400-e29b-41d4-a716-446655440001'), UUID_TO_BIN('850e8400-e29b-41d4-a716-446655440001'), UUID_TO_BIN('550e8400-e29b-41d4-a716-446655440002'), 2500, NOW(), NOW()),

-- Coffee: Alice owes Bob 1000 yen
(UUID_TO_BIN('950e8400-e29b-41d4-a716-446655440002'), UUID_TO_BIN('850e8400-e29b-41d4-a716-446655440002'), UUID_TO_BIN('550e8400-e29b-41d4-a716-446655440001'), 1000, NOW(), NOW()),

-- Repayment: Alice repays Bob (recipient is stored in transaction_history.payer_id for REPAYMENT type)
(UUID_TO_BIN('950e8400-e29b-41d4-a716-446655440003'), UUID_TO_BIN('850e8400-e29b-41d4-a716-446655440003'), UUID_TO_BIN('550e8400-e29b-41d4-a716-446655440002'), 1500, NOW(), NOW());

-- ============================================================================
-- Note: Exchange rates will be populated by calling /internal/exchange-rates API
-- Run: curl -X POST http://localhost:8080/internal/exchange-rates -H "X-API-Key: dev-api-key-local"
-- ============================================================================
