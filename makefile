git-https: # 将本地 Git 仓库从 git 协议切换到 https 协议
	git remote set-url origin https://github.com/uestc-fzk/notebook.git
	git remote -v
git-git: # 将本地 Git 仓库从 https 协议切换到 git 协议
	git remote set-url origin git@github.com:uestc-fzk/notebook.git
	git remote -v

git-proxy: # 开git代理
	git config --global http.proxy http://127.0.0.1:7890
	git config --global https.proxy https://127.0.0.1:7890
	git config --global -l
git-unproxy: # 关闭git代理
	git config --global --unset http.proxy
	git config --global --unset https.proxy
	git config --global -l
# git提交信息
msg=auto
git-push:
	echo "open git proxy, please make sure you have open vpn"
	make git-proxy
	git add .
	git commit -m "$(msg)"
	git push origin master
	echo "close git proxy"
	make git-unproxy