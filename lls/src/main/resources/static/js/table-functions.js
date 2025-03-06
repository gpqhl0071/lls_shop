/**
 * 表格功能库
 * 提供排序、搜索、虚拟滚动等表格增强功能
 */

/**
 * 按开售时间排序表格
 * @param {number} n - 要排序的列索引，即开售时间列
 * @param {string} tableId - 表格ID，默认为'productTable'
 */
function sortTable(n, tableId = 'productTable') {
    const table = document.getElementById(tableId);
    if (!table) return;

    // 从表格属性中获取当前排序状态
    let dir = table.getAttribute('data-sort-dir') || 'asc';
    // 切换排序方向
    dir = dir === 'asc' ? 'desc' : 'asc';
    
    // 设置表格排序状态
    table.setAttribute('data-sort-dir', dir);
    
    // 更新排序指示器
    const header = table.querySelectorAll('th')[n];
    const icon = header.querySelector('i.fas');
    icon.className = `fas fa-sort-${dir === 'asc' ? 'up' : 'down'} ms-auto`;

    // 获取行数据
    const tbody = table.querySelector('tbody');
    const rows = Array.from(tbody.querySelectorAll('tr'));
    
    // 如果没有数据，不执行排序
    if (rows.length <= 1) return;
    
    // 对行进行排序 - 专注于日期排序
    rows.sort((rowA, rowB) => {
        // 获取单元格
        const cellA = rowA.cells[n];
        const cellB = rowB.cells[n];
        
        // 获取日期值 - 优先使用data-sort属性
        let valueA = cellA.getAttribute('data-sort') || cellA.textContent.trim();
        let valueB = cellB.getAttribute('data-sort') || cellB.textContent.trim();
        
        // 将值转换为日期对象
        const dateA = new Date(valueA);
        const dateB = new Date(valueB);
        
        // 排序方向
        return dir === 'asc' ? dateA - dateB : dateB - dateA;
    });
    
    // 重新排列行
    rows.forEach(row => tbody.appendChild(row));
    
    // 显示排序提示信息
    const message = `已按${dir === 'asc' ? '升序' : '降序'}排列开售时间`;
    showSortMessage(message);
}

/**
 * 搜索表格内容
 * @param {string} inputId - 搜索输入框ID
 * @param {string} tableId - 表格ID
 */
function searchTable(inputId = 'searchInput', tableId = 'productTable') {
    // 使用防抖优化搜索性能
    clearTimeout(window.searchTimeout);
    window.searchTimeout = setTimeout(() => {
        const input = document.getElementById(inputId);
        const table = document.getElementById(tableId);
        if (!input || !table) return;
        
        const filter = input.value.toUpperCase();
        const tbody = table.querySelector('tbody') || table;
        const tr = tbody.getElementsByTagName("tr");
        
        // 性能优化：批量处理DOM更新
        requestAnimationFrame(() => {
            // 如果搜索为空，恢复所有行显示并退出
            if (filter === '') {
                for (let i = 0; i < tr.length; i++) {
                    tr[i].style.display = "";
                }
                return;
            }
            
            // 存储匹配计数用于高性能虚拟滚动初始化
            let matchCount = 0;
            
            for (let i = 0; i < tr.length; i++) {
                const row = tr[i];
                
                // 标题行始终显示
                if (row.classList.contains('header') || i === 0) {
                    row.style.display = "";
                    continue;
                }
                
                // 高效搜索：检查所有列
                const cells = row.getElementsByTagName("td");
                let found = false;
                
                for (let j = 0; j < cells.length; j++) {
                    const txtValue = cells[j].textContent || cells[j].innerText;
                    if (txtValue.toUpperCase().indexOf(filter) > -1) {
                        found = true;
                        break;
                    }
                }
                
                if (found) {
                    row.style.display = "";
                    matchCount++;
                    
                    // 高亮匹配内容
                    if (filter.length > 1) {
                        for (let j = 0; j < cells.length; j++) {
                            const cell = cells[j];
                            const originalHTML = cell.innerHTML;
                            const regex = new RegExp(`(${filter})`, 'gi');
                            if (regex.test(cell.textContent)) {
                                cell.innerHTML = originalHTML.replace(regex, '<mark>$1</mark>');
                                break; // 只高亮第一个匹配的单元格以提高性能
                            }
                        }
                    }
                } else {
                    row.style.display = "none";
                }
            }
            
            // 显示搜索结果计数
            const searchResultsElement = document.getElementById('searchResults');
            if (searchResultsElement) {
                searchResultsElement.textContent = `找到 ${matchCount} 条匹配结果`;
                searchResultsElement.style.display = matchCount > 0 ? '' : 'none';
            }
            
            // 初始化虚拟滚动（如果匹配项太多）
            if (matchCount > 100) {
                initVirtualScroll(tableId);
            }
        });
    }, 300); // 300ms防抖
}

/**
 * 初始化虚拟滚动
 * 当表格数据量大时提高性能
 * @param {string} tableId - 表格ID
 */
function initVirtualScroll(tableId) {
    const table = document.getElementById(tableId);
    if (!table || table.hasAttribute('data-virtual-scroll')) return;
    
    // 标记已初始化虚拟滚动
    table.setAttribute('data-virtual-scroll', 'true');
    
    const tbody = table.querySelector('tbody') || table;
    const rows = tbody.getElementsByTagName('tr');
    const visibleRows = 20; // 一次显示的行数
    const rowHeight = rows[0]?.offsetHeight || 53; // 行高估算
    
    // 创建包装容器
    const wrapper = document.createElement('div');
    wrapper.classList.add('virtual-scroll-wrapper');
    wrapper.style.overflow = 'auto';
    wrapper.style.maxHeight = `${rowHeight * visibleRows}px`;
    
    // 创建可滚动的垫片
    const spacer = document.createElement('div');
    spacer.classList.add('virtual-scroll-spacer');
    spacer.style.height = `${rowHeight * rows.length}px`;
    
    // 重组 DOM
    table.parentNode.insertBefore(wrapper, table);
    wrapper.appendChild(table);
    wrapper.appendChild(spacer);
    
    // 跟踪当前可见行
    let startIndex = 0;
    
    // 处理滚动事件
    wrapper.addEventListener('scroll', throttle(function() {
        const scrollTop = this.scrollTop;
        const newStartIndex = Math.floor(scrollTop / rowHeight);
        
        // 避免不必要的更新
        if (newStartIndex === startIndex) return;
        startIndex = newStartIndex;
        
        // 只显示可见范围内的行
        for (let i = 0; i < rows.length; i++) {
            if (i >= startIndex && i < startIndex + visibleRows + 5) { // 多渲染5行作为缓冲
                rows[i].style.display = '';
            } else {
                rows[i].style.display = 'none';
            }
        }
    }, 100));
    
    // 初始滚动事件触发
    wrapper.dispatchEvent(new Event('scroll'));
}

/**
 * 节流函数
 * 限制函数在一定时间内只能执行一次
 * @param {Function} func - 要执行的函数
 * @param {number} limit - 时间限制(ms)
 * @returns {Function} 节流后的函数
 */
function throttle(func, limit) {
    let inThrottle;
    return function() {
        const args = arguments;
        const context = this;
        if (!inThrottle) {
            func.apply(context, args);
            inThrottle = true;
            setTimeout(() => inThrottle = false, limit);
        }
    };
}

/**
 * 防抖函数
 * 延迟执行函数，如果在等待时间内再次调用则重新计时
 * @param {Function} func - 要执行的函数
 * @param {number} delay - 延迟时间(ms)
 * @returns {Function} 防抖后的函数
 */
function debounce(func, delay) {
    let debounceTimer;
    return function() {
        const context = this;
        const args = arguments;
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => func.apply(context, args), delay);
    };
}

/**
 * 表格数据缓存
 * 使用IndexedDB缓存大型表格数据
 * @param {string} tableId - 表格ID
 * @param {string} cacheKey - 缓存键名
 */
function cacheTableData(tableId, cacheKey) {
    const table = document.getElementById(tableId);
    if (!table) return;
    
    // 提取表格数据
    const data = extractTableData(table);
    
    // 使用IndexedDB存储
    if ('indexedDB' in window) {
        const request = indexedDB.open('tableCache', 1);
        
        request.onupgradeneeded = function(e) {
            const db = e.target.result;
            if (!db.objectStoreNames.contains('tables')) {
                db.createObjectStore('tables', { keyPath: 'id' });
            }
        };
        
        request.onsuccess = function(e) {
            const db = e.target.result;
            const transaction = db.transaction(['tables'], 'readwrite');
            const store = transaction.objectStore('tables');
            
            store.put({ id: cacheKey, data: data, timestamp: Date.now() });
        };
    } else {
        // 备用方案：使用localStorage
        try {
            localStorage.setItem(cacheKey, JSON.stringify({
                data: data,
                timestamp: Date.now()
            }));
        } catch (e) {
            console.error('表格缓存失败', e);
        }
    }
}

/**
 * 从表格提取数据
 * @param {HTMLElement} table - 表格元素
 * @returns {Array} 表格数据
 */
function extractTableData(table) {
    const rows = table.rows;
    const data = [];
    
    // 提取表头
    const headers = [];
    const headerRow = rows[0];
    for (let i = 0; i < headerRow.cells.length; i++) {
        headers.push(headerRow.cells[i].textContent.trim());
    }
    
    // 提取数据行
    for (let i = 1; i < rows.length; i++) {
        const rowData = {};
        const row = rows[i];
        
        for (let j = 0; j < headers.length; j++) {
            if (j < row.cells.length) {
                rowData[headers[j]] = row.cells[j].textContent.trim();
            }
        }
        
        data.push(rowData);
    }
    
    return data;
}

/**
 * 导出表格为Excel
 * @param {string} tableId - 表格ID
 * @param {string} filename - 导出文件名
 */
function exportTableToExcel(tableId, filename = 'table-data.xlsx') {
    const table = document.getElementById(tableId);
    if (!table) return;
    
    // 使用XLS格式
    const workbook = XLSX.utils.table_to_book(table);
    XLSX.writeFile(workbook, filename);
}

/**
 * 初始化可拖动列宽
 * @param {string} tableId - 表格ID
 */
function initResizableColumns(tableId) {
    const table = document.getElementById(tableId);
    if (!table) return;
    
    const headers = table.querySelectorAll('th');
    headers.forEach(header => {
        // 创建拖动手柄
        const resizer = document.createElement('div');
        resizer.classList.add('column-resizer');
        resizer.style.position = 'absolute';
        resizer.style.top = '0';
        resizer.style.right = '0';
        resizer.style.width = '5px';
        resizer.style.height = '100%';
        resizer.style.cursor = 'col-resize';
        header.style.position = 'relative';
        header.appendChild(resizer);
        
        let startX, startWidth;
        
        // 处理拖动事件
        resizer.addEventListener('mousedown', function(e) {
            startX = e.pageX;
            startWidth = header.offsetWidth;
            
            // 设置列宽拖动状态
            document.body.classList.add('resizing');
            
            document.addEventListener('mousemove', onMouseMove);
            document.addEventListener('mouseup', onMouseUp);
            
            // 防止文本选择
            e.preventDefault();
        });
        
        function onMouseMove(e) {
            const width = startWidth + (e.pageX - startX);
            header.style.width = `${width}px`;
        }
        
        function onMouseUp() {
            document.body.classList.remove('resizing');
            document.removeEventListener('mousemove', onMouseMove);
            document.removeEventListener('mouseup', onMouseUp);
            
            // 保存列宽设置到localStorage
            const colWidths = JSON.parse(localStorage.getItem(`${tableId}_colWidths`) || '{}');
            colWidths[header.cellIndex] = header.style.width;
            localStorage.setItem(`${tableId}_colWidths`, JSON.stringify(colWidths));
        }
    });
    
    // 恢复保存的列宽
    const colWidths = JSON.parse(localStorage.getItem(`${tableId}_colWidths`) || '{}');
    headers.forEach((header, index) => {
        if (colWidths[index]) {
            header.style.width = colWidths[index];
        }
    });
}

// 页面加载完成后初始化表格功能
document.addEventListener('DOMContentLoaded', function() {
    initTableFunctions();
});

/**
 * 初始化所有表格功能
 */
function initTableFunctions() {
    // 初始化表格排序
    initTableSorting();
    
    // 初始化表格搜索
    initTableSearch();
    
    // 初始化表格交互效果
    initTableInteractions();
    
    // 更新倒计时
    updateCountdowns();
    setInterval(updateCountdowns, 1000);
}

/**
 * 初始化表格排序功能
 */
function initTableSorting() {
    const tables = document.querySelectorAll('table');
    tables.forEach(table => {
        const headers = table.querySelectorAll('th[data-sort]');
        headers.forEach(header => {
            header.addEventListener('click', function() {
                const sortAttribute = this.getAttribute('data-sort');
                const currentDirection = this.getAttribute('data-direction') || 'asc';
                const newDirection = currentDirection === 'asc' ? 'desc' : 'asc';
                
                // 重置所有表头的排序状态
                headers.forEach(h => {
                    h.removeAttribute('data-direction');
                    const icon = h.querySelector('.fa-sort, .fa-sort-up, .fa-sort-down');
                    if (icon) {
                        icon.className = 'fas fa-sort ms-auto';
                    }
                });
                
                // 设置当前表头的排序状态
                this.setAttribute('data-direction', newDirection);
                const icon = this.querySelector('.fa-sort, .fa-sort-up, .fa-sort-down');
                if (icon) {
                    icon.className = `fas fa-sort-${newDirection === 'asc' ? 'up' : 'down'} ms-auto`;
                }
                
                // 执行排序
                sortTable(sortAttribute, table.id, newDirection);
            });
        });
    });
}

/**
 * 初始化表格搜索功能
 */
function initTableSearch() {
    const searchInputs = document.querySelectorAll('input[data-table]');
    searchInputs.forEach(input => {
        input.addEventListener('input', debounce(function() {
            const tableId = this.getAttribute('data-table');
            searchTable(this.value, tableId);
        }, 300));
    });
}

/**
 * 初始化表格交互效果
 */
function initTableInteractions() {
    // 添加行悬停效果
    const tableRows = document.querySelectorAll('table tbody tr');
    tableRows.forEach(row => {
        row.addEventListener('mouseenter', function() {
            this.classList.add('highlight-row');
        });
        
        row.addEventListener('mouseleave', function() {
            this.classList.remove('highlight-row');
        });
    });
    
    // 延迟加载图片
    initLazyLoading();
}

/**
 * 根据属性获取单元格索引
 * @param {HTMLElement} table - 表格元素
 * @param {string} attribute - 属性名称
 * @returns {number} - 单元格索引
 */
function getCellIndexByAttribute(table, attribute) {
    const headers = Array.from(table.querySelectorAll('th'));
    return headers.findIndex(header => header.getAttribute('data-sort') === attribute);
}

/**
 * 更新行样式
 * @param {HTMLElement} tbody - 表格主体元素
 */
function updateRowStyles(tbody) {
    const rows = Array.from(tbody.querySelectorAll('tr'));
    rows.forEach((row, index) => {
        row.classList.remove('even', 'odd');
        row.classList.add(index % 2 === 0 ? 'even' : 'odd');
    });
}

/**
 * 显示排序提示信息
 * @param {string} message - 提示信息
 */
function showSortMessage(message) {
    // 创建或获取提示元素
    let toast = document.getElementById('sort-toast');
    if (!toast) {
        toast = document.createElement('div');
        toast.id = 'sort-toast';
        toast.style.position = 'fixed';
        toast.style.bottom = '20px';
        toast.style.right = '20px';
        toast.style.backgroundColor = 'rgba(0, 123, 255, 0.9)';
        toast.style.color = 'white';
        toast.style.padding = '10px 15px';
        toast.style.borderRadius = '4px';
        toast.style.zIndex = '9999';
        toast.style.transition = 'opacity 0.5s';
        document.body.appendChild(toast);
    }
    
    // 显示消息
    toast.textContent = message;
    toast.style.opacity = '1';
    
    // 3秒后隐藏
    setTimeout(() => {
        toast.style.opacity = '0';
    }, 3000);
}

/**
 * 初始化延迟加载图片
 */
function initLazyLoading() {
    if ('IntersectionObserver' in window) {
        const imageObserver = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    const img = entry.target;
                    const src = img.getAttribute('data-src') || img.getAttribute('src');
                    
                    if (src) {
                        img.src = src;
                        img.classList.add('loaded');
                    }
                    
                    imageObserver.unobserve(img);
                }
            });
        });
        
        const lazyImages = document.querySelectorAll('img[loading="lazy"]');
        lazyImages.forEach(img => {
            imageObserver.observe(img);
        });
    }
}

/**
 * 更新倒计时
 */
function updateCountdowns() {
    const countdowns = document.getElementsByClassName('countdown');
    const now = new Date().getTime();
    
    for (let i = 0; i < countdowns.length; i++) {
        const saleTimeAttr = countdowns[i].getAttribute('data-saletime');
        if (!saleTimeAttr) continue;
        
        const saleTime = new Date(saleTimeAttr).getTime();
        const distance = saleTime - now;
        
        if (distance < 0) {
            countdowns[i].innerHTML = '<span class="badge bg-success">已开售</span>';
            countdowns[i].setAttribute('data-countdown', '0');
        } else {
            const days = Math.floor(distance / (1000 * 60 * 60 * 24));
            const hours = Math.floor((distance % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
            const minutes = Math.floor((distance % (1000 * 60 * 60)) / (1000 * 60));
            const seconds = Math.floor((distance % (1000 * 60)) / 1000);
            
            let badgeClass = 'bg-warning';
            if (distance < 1000 * 60 * 60) { // 小于1小时
                badgeClass = 'bg-danger';
            }
            
            countdowns[i].innerHTML = 
                `<span class="badge ${badgeClass}">
                    ${days > 0 ? days + '天 ' : ''}${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}
                </span>`;
            countdowns[i].setAttribute('data-countdown', distance);
        }
    }
}

/**
 * 应用过滤器
 * @param {HTMLElement} form - 表单元素
 */
function applyFilters(form) {
    // 获取所有筛选字段
    const formData = new FormData(form);
    const params = new URLSearchParams(window.location.search);
    
    // 更新URL参数
    for (const [key, value] of formData.entries()) {
        if (value && value !== '-1') {
            params.set(key, value);
        } else {
            params.delete(key);
        }
    }
    
    // 重置页码
    params.set('page', '0');
    
    // 跳转
    window.location.href = `${window.location.pathname}?${params.toString()}`;
}

/**
 * 改变每页显示数量
 * @param {number} size - 每页数量
 */
function changePageSize(size) {
    let url = new URL(window.location.href);
    let params = url.searchParams;
    
    // 保留所有其他参数，只更新size和page
    params.set('size', size);
    params.set('page', '0');  // 重置到第一页
    
    // 跳转
    window.location.href = url.toString();
}
