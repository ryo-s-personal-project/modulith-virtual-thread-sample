# リポジトリ保護設定ガイド

このリポジトリを自分以外が更新できないようにするための設定手順です。

## 方法1: ブランチ保護ルールの設定（推奨）

### 手順

1. GitHubのリポジトリページにアクセス
   - https://github.com/ryo-s-personal-project/modulith-virtual-thread-sample

2. **Settings** タブをクリック

3. 左側のメニューから **Branches** を選択

4. **Branch protection rules** セクションで **Add rule** をクリック

5. 以下の設定を行います：

   **Branch name pattern:**
   - `*` (すべてのブランチ) または `main`、`master`、`add-benchmark-endpoints` など特定のブランチ名

   **Protect matching branches の設定:**
   - ✅ **Require a pull request before merging** を有効化
   - ✅ **Require approvals** を有効化（承認者数を1に設定）
   - ✅ **Restrict who can push to matching branches** を有効化
     - 自分のアカウントのみを許可リストに追加

   **Restrict pushes that create files larger than 100 MB** を有効化（オプション）

6. **Create** をクリックして保存

### 効果

- 指定したブランチへの直接プッシュが制限されます
- プルリクエスト経由でのみ変更が可能になります
- 承認が必要になります（自分が承認者として設定）

## 方法2: リポジトリのアクセス権限を制限

### 手順

1. GitHubのリポジトリページにアクセス

2. **Settings** タブをクリック

3. 左側のメニューから **Collaborators** を選択

4. **Add people** ボタンで追加されたコラボレーターを確認
   - 不要なコラボレーターがいる場合は削除

5. リポジトリの可視性を確認
   - **Settings** → **General** → **Danger Zone** → **Change visibility**
   - 必要に応じて **Private** に変更

### 効果

- リポジトリへのアクセス権限を持つユーザーを制限できます
- プライベートリポジトリにすることで、より厳格に制御できます

## 方法3: GitHub CLIを使用した設定（CLIがインストールされている場合）

```bash
# GitHub CLIでログイン
gh auth login

# ブランチ保護ルールを設定
gh api repos/ryo-s-personal-project/modulith-virtual-thread-sample/branches/main/protection \
  --method PUT \
  --field required_status_checks='{"strict":true,"contexts":[]}' \
  --field enforce_admins=true \
  --field required_pull_request_reviews='{"dismissal_restrictions":{},"dismiss_stale_reviews":true,"require_code_owner_reviews":false,"required_approving_review_count":1}' \
  --field restrictions='{"users":["YOUR_USERNAME"],"teams":[]}'
```

## 推奨設定

以下の組み合わせを推奨します：

1. **ブランチ保護ルール**を設定（方法1）
   - すべてのブランチまたは主要ブランチを保護
   - 直接プッシュを禁止
   - プルリクエストと承認を必須化

2. **リポジトリをプライベート化**（方法2）
   - 不要なアクセスを防止

3. **コラボレーターの管理**（方法2）
   - 必要なユーザーのみを追加

## 注意事項

- ブランチ保護ルールを設定すると、自分自身も直接プッシュできなくなる場合があります
- その場合は、プルリクエストを作成して自分で承認する必要があります
- または、保護ルールで自分のアカウントを例外として追加できます

## 現在の設定確認

現在のブランチ保護ルールを確認するには：

1. GitHubのリポジトリページで **Settings** → **Branches** を確認
2. または、以下のコマンドで確認（GitHub CLIがインストールされている場合）：
   ```bash
   gh api repos/ryo-s-personal-project/modulith-virtual-thread-sample/branches/main/protection
   ```
