/**
 * 产品详情页面的JavaScript文件
 * 提供产品图片预览、延迟加载和交互功能
 */
document.addEventListener('DOMContentLoaded', function() {
    initImageZoom();
    initLazyLoading();
    setupQuantityControls();
    trackProductView();
});

/**
 * 初始化图片放大预览功能
 */
function initImageZoom() {
    const productImage = document.querySelector('.product-image');
    if (!productImage) return;

    productImage.addEventListener('click', function() {
        // 创建模态框
        const modal = document.createElement('div');
        modal.classList.add('image-zoom-modal');
        modal.style.position = 'fixed';
        modal.style.top = '0';
        modal.style.left = '0';
        modal.style.width = '100%';
        modal.style.height = '100%';
        modal.style.backgroundColor = 'rgba(0,0,0,0.9)';
        modal.style.zIndex = '1050';
        modal.style.display = 'flex';
        modal.style.alignItems = 'center';
        modal.style.justifyContent = 'center';
        modal.style.cursor = 'zoom-out';

        // 创建放大的图片
        const zoomedImage = document.createElement('img');
        zoomedImage.src = this.src;
        zoomedImage.style.maxHeight = '90%';
        zoomedImage.style.maxWidth = '90%';
        zoomedImage.style.objectFit = 'contain';
        zoomedImage.style.border = '5px solid #fff';
        zoomedImage.style.boxShadow = '0 0 20px rgba(0,0,0,0.5)';
        
        // 添加到DOM
        modal.appendChild(zoomedImage);
        document.body.appendChild(modal);
        
        // 点击关闭模态框
        modal.addEventListener('click', function() {
            document.body.removeChild(modal);
        });
    });
}

/**
 * 初始化图片懒加载
 */
function initLazyLoading() {
    // 检查浏览器是否支持IntersectionObserver
    if ('IntersectionObserver' in window) {
        const lazyImages = document.querySelectorAll('img[loading="lazy"]');
        
        const imageObserver = new IntersectionObserver(function(entries, observer) {
            entries.forEach(function(entry) {
                if (entry.isIntersecting) {
                    const img = entry.target;
                    img.src = img.dataset.src || img.src;
                    img.classList.add('loaded');
                    imageObserver.unobserve(img);
                }
            });
        });
        
        lazyImages.forEach(function(image) {
            imageObserver.observe(image);
        });
    }
}

/**
 * 设置数量控制器
 */
function setupQuantityControls() {
    const quantityInput = document.querySelector('.quantity-input');
    if (!quantityInput) return;
    
    const decreaseBtn = document.querySelector('.quantity-decrease');
    const increaseBtn = document.querySelector('.quantity-increase');
    
    if (decreaseBtn) {
        decreaseBtn.addEventListener('click', function() {
            const currentValue = parseInt(quantityInput.value);
            if (currentValue > 1) {
                quantityInput.value = currentValue - 1;
                // 触发change事件以更新价格
                quantityInput.dispatchEvent(new Event('change'));
            }
        });
    }
    
    if (increaseBtn) {
        increaseBtn.addEventListener('click', function() {
            const currentValue = parseInt(quantityInput.value);
            const maxStock = parseInt(quantityInput.dataset.maxStock || '999');
            if (currentValue < maxStock) {
                quantityInput.value = currentValue + 1;
                // 触发change事件以更新价格
                quantityInput.dispatchEvent(new Event('change'));
            }
        });
    }
    
    if (quantityInput) {
        quantityInput.addEventListener('change', function() {
            updateTotalPrice();
        });
    }
}

/**
 * 更新总价
 */
function updateTotalPrice() {
    const quantityInput = document.querySelector('.quantity-input');
    const priceElement = document.querySelector('.current-price');
    const totalPriceElement = document.querySelector('.total-price');
    
    if (!quantityInput || !priceElement || !totalPriceElement) return;
    
    const quantity = parseInt(quantityInput.value);
    const price = parseFloat(priceElement.textContent.replace('¥', ''));
    const totalPrice = (quantity * price).toFixed(2);
    
    totalPriceElement.textContent = '¥' + totalPrice;
}

/**
 * 记录产品浏览数据
 */
function trackProductView() {
    const productId = document.querySelector('[data-product-id]')?.dataset.productId;
    if (!productId) return;
    
    // 使用浏览器存储记录最近浏览的产品
    try {
        const viewedProducts = JSON.parse(localStorage.getItem('viewedProducts') || '[]');
        if (!viewedProducts.includes(productId)) {
            viewedProducts.unshift(productId);
            // 只保留最近10个
            viewedProducts.splice(10);
            localStorage.setItem('viewedProducts', JSON.stringify(viewedProducts));
        }
        
        // 如果有API，可以发送浏览记录到服务器
        fetch('/api/products/view', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ productId }),
            // 使用keepalive确保页面关闭时请求仍能完成
            keepalive: true
        }).catch(() => {
            // 静默失败，不影响用户体验
        });
    } catch (e) {
        console.error('无法跟踪产品浏览数据', e);
    }
}

/**
 * 添加到购物车
 */
document.addEventListener('DOMContentLoaded', function() {
    const addToCartBtn = document.querySelector('.btn-primary');
    if (addToCartBtn) {
        addToCartBtn.addEventListener('click', function(e) {
            e.preventDefault();
            const productId = document.querySelector('[data-product-id]')?.dataset.productId;
            const quantity = document.querySelector('.quantity-input')?.value || 1;
            
            // 显示加载状态
            addToCartBtn.disabled = true;
            addToCartBtn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> 添加中...';
            
            // 模拟添加到购物车
            setTimeout(() => {
                // 恢复按钮状态
                addToCartBtn.disabled = false;
                addToCartBtn.innerHTML = '<i class="fas fa-shopping-cart me-2"></i>加入购物车';
                
                // 显示成功提示
                const toast = document.createElement('div');
                toast.classList.add('toast', 'align-items-center', 'text-white', 'bg-success', 'border-0');
                toast.setAttribute('role', 'alert');
                toast.setAttribute('aria-live', 'assertive');
                toast.setAttribute('aria-atomic', 'true');
                toast.style.position = 'fixed';
                toast.style.top = '20px';
                toast.style.right = '20px';
                toast.style.zIndex = '1050';
                
                toast.innerHTML = `
                    <div class="d-flex">
                        <div class="toast-body">
                            <i class="fas fa-check-circle me-2"></i> 商品已成功加入购物车！
                        </div>
                        <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="关闭"></button>
                    </div>
                `;
                
                document.body.appendChild(toast);
                const bsToast = new bootstrap.Toast(toast, { delay: 3000 });
                bsToast.show();
                
                // 关闭后删除元素
                toast.addEventListener('hidden.bs.toast', function() {
                    document.body.removeChild(toast);
                });
            }, 800);
        });
    }
});

/**
 * 性能优化：节流函数
 * 限制函数在一定时间内只能执行一次
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
 * 性能优化：防抖函数
 * 延迟执行函数，如果在等待时间内再次调用则重新计时
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

// 对滚动事件进行节流处理
window.addEventListener('scroll', throttle(function() {
    // 滚动处理逻辑
}, 100));

// 对调整窗口大小事件进行防抖处理
window.addEventListener('resize', debounce(function() {
    // 调整窗口大小处理逻辑
}, 250)); 